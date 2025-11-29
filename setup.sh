#!/bin/bash

echo "Installing cargo-nx..."

# 先确保rust环境已设置好
if ! command -v cargo &> /dev/null
then
    echo "Cargo not found, installing rust..."
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
    source "$HOME/.cargo/env"
    
    # 确保环境变量生效
    export PATH="$HOME/.cargo/bin:$PATH"
    
    if ! command -v cargo &> /dev/null
    then
        echo "Failed to install or setup Rust properly"
        exit 1
    fi
fi

# 创建临时目录用于修复cargo-nx
TEMP_DIR=$(mktemp -d)
echo "Created temporary directory: $TEMP_DIR"

# 使用不同的协议尝试克隆仓库到临时目录
echo "Cloning cargo-nx repository..."
if ! git -c http.sslVerify=false clone https://github.com/aarch64-switch-rs/cargo-nx "$TEMP_DIR/cargo-nx"; then
    echo "HTTPS clone failed, trying with http.proxy option disabled..."
    if ! git -c http.proxy= -c https.proxy= clone https://github.com/aarch64-switch-rs/cargo-nx "$TEMP_DIR/cargo-nx"; then
        echo "All clone attempts failed"
        rm -rf "$TEMP_DIR"
        exit 1
    fi
fi

# 进入克隆的目录
cd "$TEMP_DIR/cargo-nx"

# 检查模板文件是否存在
if [[ ! -f default/nsp/Cargo.toml ]] || [[ ! -f default/nro/Cargo.toml ]] || [[ ! -f default/lib/Cargo.toml ]]; then
    echo "Template files not found in expected locations"
    ls -R default/
    cd /
    rm -rf "$TEMP_DIR"
    exit 1
fi

# 修复模板文件中的包名
echo "Fixing template files..."
sed -i 's/name = "<name>"/name = "template"/g' default/nsp/Cargo.toml
sed -i 's/name = "<name>"/name = "template"/g' default/nro/Cargo.toml
sed -i 's/name = "<name>"/name = "template"/g' default/lib/Cargo.toml
sed -i 's/default_name = "<name>"/default_name = "template"/g' default/nro/Cargo.toml
sed -i 's/name = "<name>"/name = "template"/g' default/nsp/Cargo.toml

# 从本地修复后的源码安装cargo-nx
echo "Installing cargo-nx from fixed source..."
if ! cargo install --path .; then
    echo "Failed to install cargo-nx"
    cd /
    rm -rf "$TEMP_DIR"
    exit 1
fi

# 清理临时目录
cd /
rm -rf "$TEMP_DIR"

echo "cargo-nx installed successfully!"