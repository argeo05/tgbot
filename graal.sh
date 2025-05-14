  # GraalVM
  GRAAL_DIR="/opt/graalvm"
  if [ ! -d "$GRAAL_DIR/graalvm-jdk-24" ]; then
    echo "Installing GraalVM..."
    wget -nc https://download.oracle.com/graalvm/24/latest/graalvm-jdk-24_linux-x64_bin.tar.gz
    TAR_FILE=$(basename "$(ls -t graalvm-jdk-24*tar.gz | head -n 1)")
    tar -xvf "$TAR_FILE"
    DIR_NAME=$(tar -tf "$TAR_FILE" | head -n 1 | cut -d/ -f1)
    $SUDO mkdir -p "$GRAAL_DIR"
    $SUDO mv "$DIR_NAME" "$GRAAL_DIR/graalvm-jdk-24"
  else
    echo "GraalVM is already installed."
  fi

  export GRAALVM_HOME="$GRAAL_DIR/graalvm-jdk-24"
  export JAVA_HOME="$GRAALVM_HOME"
