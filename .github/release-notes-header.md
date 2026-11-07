## Installing

Download the file for your system from **Assets** below. The Downloads section of the [README](https://github.com/Anthony17serrato/Phovo#readme) lists which file fits which system.

**macOS:** the app isn't signed by Apple yet, so the first launch is blocked with **"Phovo" Not Opened**.
1. Open the DMG and drag Phovo to Applications.
2. Open Phovo. When the "Not Opened" message appears, click **Done** (not Move to Trash).
3. Open **System Settings → Privacy & Security** and scroll down to Security.
4. Next to "Phovo was blocked to protect your Mac", click **Open Anyway** and confirm with your password or Touch ID.
5. Click **Open**. You only need to do this once per version.

**Windows:** if "Windows protected your PC" appears, click **More info**, then **Run anyway**.

**Android:** open the `.apk` on your phone and allow installing apps from your browser or file manager when asked.

**Linux:**
- Ubuntu, Debian, Mint: `sudo apt install ./Phovo-<version>-linux-<arch>.deb`
- Fedora, RHEL, openSUSE: `sudo dnf install ./Phovo-<version>-linux-x64.rpm` (or `sudo zypper install` on openSUSE)
- Any distro: unpack the `.tar.gz` and run `Phovo/bin/Phovo`
- Raspberry Pi: use the `linux-arm64` files on 64-bit Raspberry Pi OS

**Checking a download:** in the folder you downloaded to, `shasum -a 256 -c SHA256SUMS --ignore-missing` (macOS) or `sha256sum -c SHA256SUMS --ignore-missing` (Linux) should print `OK` for each file.

