#!/usr/bin/env python3
"""
Zero-dependency HTTP server for the Tradee (TradeLink) Website Codebase.
Run: python server.py [port]
"""
import http.server
import os
import socketserver
import sys

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
WEB_DIR = os.path.dirname(os.path.abspath(__file__))

class TradeeRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

if __name__ == "__main__":
    with socketserver.TCPServer(("127.0.0.1", PORT), TradeeRequestHandler) as httpd:
        print(f"Tradee Website running at http://127.0.0.1:{PORT}")
        httpd.serve_forever()
