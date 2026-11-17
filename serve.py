#!/usr/bin/env python3
"""Server statis lokal untuk Gofile Expressive (tanpa build step)."""

import argparse
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent


class Handler(SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Cache-Control", "no-cache")
        super().end_headers()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=5173)
    args = parser.parse_args()

    handler = partial(Handler, directory=str(ROOT))
    server = ThreadingHTTPServer(("0.0.0.0", args.port), handler)
    print(f"Gofile Expressive -> http://localhost:{args.port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nSelesai.")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
