#!/bin/sh

install -m 644 -D "target/eclipse-format.jar" "/usr/local/share/eclipse-format/eclipse-format.jar"
install -m 755 -D "src/main/sh/eclipse-format" "/usr/bin/eclipse-format"
