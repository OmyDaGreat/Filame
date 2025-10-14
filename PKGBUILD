# Maintainer: Om Gupta <omgupta@example.com>
pkgname=filame
pkgver=1.0.0
pkgrel=1
pkgdesc="A powerful terminal tool for managing Arch Linux configurations across multiple devices"
arch=('any')
url="https://github.com/OmyDaGreat/Filame"
license=('MIT')
depends=('java-runtime>=21')
makedepends=('gradle' 'git')
source=("$pkgname-$pkgver.tar.gz::https://github.com/OmyDaGreat/Filame/archive/v$pkgver.tar.gz")
sha256sums=('SKIP')

build() {
    cd "$srcdir/Filame-$pkgver"
    gradle build --no-daemon
}

package() {
    cd "$srcdir/Filame-$pkgver"
    
    # Create installation directories
    install -dm755 "$pkgdir/usr/share/java/$pkgname"
    install -dm755 "$pkgdir/usr/bin"
    
    # Install the JAR file
    install -Dm644 app/build/libs/runner.jar "$pkgdir/usr/share/java/$pkgname/$pkgname.jar"
    
    # Create launcher script
    cat > "$pkgdir/usr/bin/$pkgname" << EOF
#!/bin/sh
exec java -jar /usr/share/java/$pkgname/$pkgname.jar "\$@"
EOF
    chmod +x "$pkgdir/usr/bin/$pkgname"
    
    # Install license
    install -Dm644 LICENSE "$pkgdir/usr/share/licenses/$pkgname/LICENSE"
    
    # Install documentation
    install -Dm644 README.adoc "$pkgdir/usr/share/doc/$pkgname/README.adoc"
}
