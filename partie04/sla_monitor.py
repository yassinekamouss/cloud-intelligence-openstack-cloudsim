#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
===============================================================================
Projet Cloud Intelligence — Partie 4 : Service Level Agreement (SLA) Monitor
Étudiant : Yassine Kamouss (ID: R140087494)
Description :
  Programme d'ingénierie SRE qui se connecte à OpenStack via `openstacksdk`
  (authentification Keystone via clouds.yaml), vérifie périodiquement l'état
  des instances cibles (`instance-cirros-sla1` et `instance-cirros-sla2`) via
  l'API Nova ainsi que leur connectivité réseau (ICMP Ping / TCP Port 22 SSH),
  journalise chaque mesure dans `measurements.log`, calcule la disponibilité
  en temps réel et met à jour automatiquement le rapport dans `sla.json`.
===============================================================================
"""

import argparse
import datetime
import json
import os
import socket
import subprocess
import sys
import time
from typing import Dict, List, Optional, Tuple

import openstack


DEFAULT_INSTANCES = ["instance-cirros-sla1", "instance-cirros-sla2"]
SLA_TARGET_PERCENT = 99.5


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Surveillance SLA OpenStack (API Nova + Sonde Réseau ICMP/SSH)"
    )
    parser.add_argument(
        "--cloud",
        default="devstack-admin",
        help="Nom du profil OpenStack dans /etc/openstack/clouds.yaml (défaut: devstack-admin)",
    )
    parser.add_argument(
        "--instances",
        nargs="+",
        default=DEFAULT_INSTANCES,
        help="Liste des instances à surveiller (défaut: instance-cirros-sla1 instance-cirros-sla2)",
    )
    parser.add_argument(
        "--interval",
        type=int,
        default=300,
        help="Fréquence de mesure en secondes (défaut: 300s = 5 minutes)",
    )
    parser.add_argument(
        "--cycles",
        type=int,
        default=12,
        help="Nombre total de cycles de mesure à effectuer (défaut: 12 cycles)",
    )
    parser.add_argument(
        "--sla-file",
        default="sla.json",
        help="Chemin du fichier contrat SLA JSON à mettre à jour (défaut: sla.json)",
    )
    parser.add_argument(
        "--log-file",
        default="measurements.log",
        help="Chemin du journal des mesures (défaut: measurements.log)",
    )
    return parser.parse_args()


def extract_instance_ips(server) -> Tuple[Optional[str], Optional[str]]:
    """Extrait l'adresse IP privée (fixed) et l'adresse IP flottante (floating) d'une instance."""
    fixed_ip = None
    floating_ip = None
    addresses = getattr(server, "addresses", {}) or {}
    for net_name, addr_list in addresses.items():
        for addr_info in addr_list:
            ip = addr_info.get("addr")
            ip_type = addr_info.get("OS-EXT-IPS:type")
            if ip_type == "floating":
                floating_ip = ip
            elif ip_type == "fixed" and not fixed_ip:
                fixed_ip = ip
            elif not fixed_ip:
                fixed_ip = ip
    return fixed_ip, floating_ip


def check_icmp_ping(ip_address: str, timeout: int = 2) -> bool:
    """Teste la connectivité ICMP (ping) vers l'adresse IP."""
    try:
        result = subprocess.run(
            ["ping", "-c", "1", "-W", str(timeout), ip_address],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=False,
        )
        return result.returncode == 0
    except Exception:
        return False


def check_tcp_ssh(ip_address: str, port: int = 22, timeout: float = 2.0) -> bool:
    """Teste l'ouverture du port TCP 22 (SSH) vers l'adresse IP."""
    try:
        with socket.create_connection((ip_address, port), timeout=timeout):
            return True
    except Exception:
        return False


def evaluate_penalty(availability: float, target: float = SLA_TARGET_PERCENT) -> Tuple[bool, str, str]:
    """Détermine si le SLA est respecté et la pénalité applicable selon le contrat."""
    if availability >= target:
        return True, "RESPECTÉ (CONFORME)", "0% (Aucune pénalité)"
    elif availability >= 99.0:
        return False, "NON RESPECTÉ (VIOLATION MINEURE)", "10% d'avoir (Service Credit)"
    elif availability >= 95.0:
        return False, "NON RESPECTÉ (VIOLATION MAJEURE)", "25% d'avoir + Post-Mortem"
    else:
        return False, "NON RESPECTÉ (VIOLATION CRITIQUE)", "50% d'avoir + Migration prioritaire"


def update_sla_file(
    sla_path: str,
    start_time_iso: str,
    end_time_iso: str,
    interval_sec: int,
    cycles_completed: int,
    stats: Dict[str, Dict],
) -> None:
    """Met à jour la section surveillance_report dans le fichier sla.json."""
    if os.path.exists(sla_path):
        with open(sla_path, "r", encoding="utf-8") as f:
            sla_data = json.load(f)
    else:
        sla_data = {}

    instances_summary = {}
    all_compliant = True

    for inst_name, data in stats.items():
        total = data["total_checks"]
        up = data["available_checks"]
        down = data["unavailable_checks"]
        availability = round((up / total) * 100.0, 2) if total > 0 else 0.0
        compliant, verdict, penalty = evaluate_penalty(availability)
        if not compliant:
            all_compliant = False

        instances_summary[inst_name] = {
            "total_measurements": total,
            "available_measurements": up,
            "unavailable_measurements": down,
            "availability_percent": availability,
            "sla_target_percent": SLA_TARGET_PERCENT,
            "objective_met": compliant,
            "verdict": verdict,
            "applicable_penalty": penalty,
            "last_known_nova_status": data["last_nova_status"],
            "last_known_ip": data["last_ip"],
        }

    sla_data["surveillance_report"] = {
        "evaluation_period": {
            "start_utc": start_time_iso,
            "end_utc": end_time_iso,
            "sampling_interval_seconds": interval_sec,
            "total_cycles_executed": cycles_completed,
            "extrapolation_note": (
                "Calcul basé sur la formule contractuelle : "
                "(Mesures_Disponibles / Mesures_Totales) * 100. "
                "Sur une journée complète (24h à 1 mesure / 5 min = 288 mesures), "
                "un objectif de 99,5% exige au moins 287 mesures disponibles "
                "(tolérance maximale d'une seule mesure en échec, soit 5 minutes de panne)."
            ),
        },
        "global_sla_compliance": all_compliant,
        "global_verdict": (
            "OBJECTIF SLA GLOBAL RESPECTÉ"
            if all_compliant
            else "VIOLATION DE SLA DÉTECTÉE SUR AU MOINS UNE INSTANCE"
        ),
        "instances_results": instances_summary,
    }

    with open(sla_path, "w", encoding="utf-8") as f:
        json.dump(sla_data, f, indent=2, ensure_ascii=False)


def main() -> int:
    args = parse_args()

    print("=" * 88)
    print("  DÉMON DE SURVEILLANCE SLA OPENSTACK (Keystone + API Nova + Sonde Réseau ICMP/SSH)")
    print(f"  Étudiant : Yassine Kamouss (R140087494) | Profil Cloud : {args.cloud}")
    print(f"  Instances surveillées : {', '.join(args.instances)}")
    print(f"  Objectif SLA : {SLA_TARGET_PERCENT}% | Intervalle : {args.interval}s | Cycles prévus : {args.cycles}")
    print("=" * 88)

    try:
        conn = openstack.connect(cloud=args.cloud)
    except Exception as exc:
        print(f"[ERREUR CRITIQUE] Échec de connexion OpenStack ({args.cloud}) : {exc}", file=sys.stderr)
        return 1

    stats: Dict[str, Dict] = {
        name: {
            "total_checks": 0,
            "available_checks": 0,
            "unavailable_checks": 0,
            "last_nova_status": "UNKNOWN",
            "last_ip": "N/A",
        }
        for name in args.instances
    }

    start_time_iso = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    # Initialisation de l'en-tête du journal des mesures s'il n'existe pas
    if not os.path.exists(args.log_file) or os.path.getsize(args.log_file) == 0:
        with open(args.log_file, "a", encoding="utf-8") as lf:
            lf.write(
                "# Journal de Surveillance SLA OpenStack — Yassine Kamouss (R140087494)\n"
                "# Format : TIMESTAMP_UTC | CYCLE | INSTANCE | NOVA_STATUS | IP_CIBLE | PING_ICMP | SSH_TCP22 | ETAT_SLA | DISPONIBILITE_CUMULEE\n"
            )

    try:
        for cycle in range(1, args.cycles + 1):
            now_iso = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
            print(f"\n--- [Cycle {cycle}/{args.cycles}] Horodatage UTC : {now_iso} ---")

            for inst_name in args.instances:
                server = conn.compute.find_server(inst_name, ignore_missing=True)
                if server is None:
                    nova_status = "NOT_FOUND"
                    target_ip = "N/A"
                    ping_ok = False
                    ssh_ok = False
                    is_available = False
                else:
                    # Rafraîchir les détails complets de l'instance depuis l'API Nova
                    server = conn.compute.get_server(server.id)
                    nova_status = str(server.status).upper()
                    fixed_ip, floating_ip = extract_instance_ips(server)
                    target_ip = floating_ip or fixed_ip or "N/A"

                    if nova_status == "ACTIVE" and target_ip != "N/A":
                        ping_ok = check_icmp_ping(target_ip)
                        ssh_ok = check_tcp_ssh(target_ip, port=22)
                        is_available = ping_ok or ssh_ok
                    else:
                        ping_ok = False
                        ssh_ok = False
                        is_available = False

                # Mise à jour des compteurs statistiques
                stats[inst_name]["total_checks"] += 1
                stats[inst_name]["last_nova_status"] = nova_status
                stats[inst_name]["last_ip"] = target_ip

                if is_available:
                    stats[inst_name]["available_checks"] += 1
                    sla_state = "DISPONIBLE"
                else:
                    stats[inst_name]["unavailable_checks"] += 1
                    sla_state = "INDISPONIBLE"

                total_c = stats[inst_name]["total_checks"]
                up_c = stats[inst_name]["available_checks"]
                avail_pct = (up_c / total_c) * 100.0
                compliant = avail_pct >= SLA_TARGET_PERCENT

                # Écriture dans le journal des mesures (measurements.log)
                log_line = (
                    f"{now_iso} | Cycle={cycle:02d}/{args.cycles:02d} | "
                    f"Instance={inst_name:<20} | Nova={nova_status:<8} | "
                    f"IP={target_ip:<15} | Ping={'OK' if ping_ok else 'KO'} | "
                    f"SSH={'OK' if ssh_ok else 'KO'} | "
                    f"Etat={sla_state:<12} | Dispo={avail_pct:6.2f}% ({up_c}/{total_c}) | "
                    f"SLA={'OK' if compliant else 'BREACH'}\n"
                )
                with open(args.log_file, "a", encoding="utf-8") as lf:
                    lf.write(log_line)

                # Affichage console clair (idéal pour la capture de détection de panne)
                badge = "[OK - DISPONIBLE]" if is_available else "[ALERTE PANNE - INDISPONIBLE]"
                sla_tag = "SLA RESPECTÉ (>=99.5%)" if compliant else "VIOLATION SLA (<99.5%)"
                print(
                    f"  * {inst_name:<20} : {badge:<30} | Nova={nova_status:<7} | "
                    f"IP={target_ip:<13} | Ping={'OK' if ping_ok else 'KO'}/SSH={'OK' if ssh_ok else 'KO'} | "
                    f"Disponibilité = {avail_pct:6.2f}% ({up_c}/{total_c}) -> {sla_tag}"
                )

            end_time_iso = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
            update_sla_file(
                sla_path=args.sla_file,
                start_time_iso=start_time_iso,
                end_time_iso=end_time_iso,
                interval_sec=args.interval,
                cycles_completed=cycle,
                stats=stats,
            )

            if cycle < args.cycles:
                print(f"  [Attente de {args.interval} secondes avant la prochaine sonde...]")
                time.sleep(args.interval)

    except KeyboardInterrupt:
        print("\n[INFO] Interruption manuelle détectée (Ctrl+C). Sauvegarde finale du rapport SLA...")

    print("\n" + "=" * 88)
    print("  BILAN FINAL DE SURVEILLANCE SLA (Enregistré dans sla.json et measurements.log)")
    print("=" * 88)
    for inst_name, d in stats.items():
        total = d["total_checks"]
        up = d["available_checks"]
        avail = (up / total) * 100.0 if total > 0 else 0.0
        compliant, verdict, penalty = evaluate_penalty(avail)
        print(
            f"  - {inst_name:<20} : {up}/{total} mesures disponibles | "
            f"Disponibilité = {avail:6.2f}% | Verdict : {verdict} | Pénalité : {penalty}"
        )
    print("=" * 88)
    return 0


if __name__ == "__main__":
    sys.exit(main())
