import argparse
import requests
import time
import csv
import threading
import json
import sys

BASE_URL = "http://localhost:8080/bitcask"

def view_all(filename=None):
    try:
        response = requests.get(f"{BASE_URL}/all")
        response.raise_for_status()

        if response.status_code == 204:
            print("Bitcask is currently empty.")
            return

        data = response.json()
        ts = int(time.time())
        filename = filename or f"./data/{ts}.csv"

        with open(filename, mode='w', newline='') as file:
            writer = csv.writer(file)
            writer.writerow(["key", "value"])
            for item in data:
                writer.writerow([item["station_id"], json.dumps(item)])

        print(f"Success! Wrote all keys to {filename}")
    except requests.exceptions.RequestException as e:
        print(f"Error connecting to Central Station: {e}")


def view_key(key):
    try:
        response = requests.get(f"{BASE_URL}/{key}")
        if response.status_code == 200:
            print(json.dumps(response.json()))

        elif response.status_code == 404:
            print(f"Key '{key}' not found in Bitcask.")
        else:

            print(f"Error: Received HTTP {response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"Error fetching key {key}: {e}")


def perf_worker(thread_id):
    ts = int(time.time())
    filename = f"./threads/{ts}_thread_{thread_id}.csv"

    view_all(filename)
    print(f"Thread {thread_id} finished writing to {filename}")


def perf_test(clients):

    print(f"Starting {clients} threads. Each thread will query all keys...")

    
    threads = []
    for i in range(1, clients + 1):
        t = threading.Thread(target=perf_worker, args=(i,))
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    print("Performance test complete!")

def main():
    parser = argparse.ArgumentParser(description="Bitcask Client Application")
    parser.add_argument("--view-all", action="store_true", help="View all keys and values, output to CSV")
    parser.add_argument("--view", action="store_true", help="View a specific key")
    parser.add_argument("--key", type=str, help="The key to view")
    parser.add_argument("--perf", action="store_true", help="Run performance test")
    parser.add_argument("--clients", type=int, help="Number of concurrent clients for perf test")

    args = parser.parse_args()

    if args.view_all:
        view_all()
    elif args.view:
        if not args.key:
            print("Error: --key is required when using --view")
            sys.exit(1)
        view_key(args.key)
    elif args.perf:
        if not args.clients:
            print("Error: --clients is required when using --perf")
            sys.exit(1)
        perf_test(args.clients)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()