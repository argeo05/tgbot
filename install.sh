#!/bin/bash
set -e

BINARY_NAME="tgs"
INSTALL_DIR="/usr/local/bin"


install_binary() {
    echo "Installing binary..."
    chmod +x app/target/tgs
    sudo install -m 0755 \
        app/target/tgs \
        "$INSTALL_DIR/$BINARY_NAME"
}

main() {
    install_binary
    echo "Installation complete! Run with: $BINARY_NAME"
}

main