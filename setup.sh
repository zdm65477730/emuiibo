#!/bin/bash

echo "Installing cargo-nx..."

if ! command -v cargo &> /dev/null
then
    echo "Cargo not found, installing rust..."
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
    source "$HOME/.cargo/env"

    export PATH="$HOME/.cargo/bin:$PATH"

    if ! command -v cargo &> /dev/null
    then
        echo "Failed to install or setup Rust properly"
        exit 1
    fi
fi

TEMP_DIR=$(mktemp -d)
echo "Created temporary directory: $TEMP_DIR"

echo "Cloning cargo-nx repository..."
if ! git -c http.sslVerify=false clone https://github.com/aarch64-switch-rs/cargo-nx "$TEMP_DIR/cargo-nx"; then
    echo "HTTPS clone failed, trying with http.proxy option disabled..."
    if ! git -c http.proxy= -c https.proxy= clone https://github.com/aarch64-switch-rs/cargo-nx "$TEMP_DIR/cargo-nx"; then
        echo "All clone attempts failed"
        rm -rf "$TEMP_DIR"
        exit 1
    fi
fi

cd "$TEMP_DIR/cargo-nx"

if [[ ! -f default/nsp/Cargo.toml ]] || [[ ! -f default/nro/Cargo.toml ]] || [[ ! -f default/lib/Cargo.toml ]]; then
    echo "Template files not found in expected locations"
    ls -R default/
    cd /
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo "Fixing template files..."
sed -i 's/name = "<name>"/name = "template"/g' default/nsp/Cargo.toml
sed -i 's/name = "<name>"/name = "template"/g' default/nro/Cargo.toml
sed -i 's/name = "<name>"/name = "template"/g' default/lib/Cargo.toml
sed -i 's/default_name = "<name>"/default_name = "template"/g' default/nro/Cargo.toml
sed -i 's/name = "<name>"/name = "template"/g' default/nsp/Cargo.toml

echo "Installing cargo-nx from fixed source..."
if ! cargo install --path .; then
    echo "Failed to install cargo-nx"
    cd /
    rm -rf "$TEMP_DIR"
    exit 1
fi

cd /
rm -rf "$TEMP_DIR"

echo "cargo-nx installed successfully!"