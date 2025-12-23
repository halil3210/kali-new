package alie.info.newmultichoice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kali_tools")
data class KaliTool(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String,
    val mainFunction: String,
    val importance: String,
    val runCommand: String,
    val guiPath: String,
    val usage: String,
    val note: String,
    val category: String, // e.g., "Sniffing & Spoofing", "Exploitation", "Password Cracking"
    val iconResId: Int = 0 // For local drawable icons
)

object KaliTools {
    fun getAllTools(): List<KaliTool> = listOf(
        KaliTool(
            id = 1,
            name = "Wireshark",
            description = "Network protocol analyzer",
            mainFunction = "This tool, its main function is to capture data passing through the network, whether it is pictures or websites visited by the user...etc.",
            importance = "This tool is important for those who want to enter the world of information security and protect their networks.",
            runCommand = "sudo wireshark",
            guiPath = "Kali Linux icon → Sniffing & Spoofing Tools → Wireshark",
            usage = "As you notice in front of you in the picture, there is the word eth0 and wlan0, and there is a graph in front of them, which symbolizes that data is being received. These two words refer to the Wi-Fi network of the Kali Linux system. Choose from one of these two options.",
            note = "If you are using a default Kali Linux system, you will need a wireless Wi-Fi adapter to run the scan.",
            category = "Sniffing & Spoofing"
        ),
        KaliTool(
            id = 2,
            name = "Metasploit Framework",
            description = "Penetration testing platform",
            mainFunction = "The Metasploit Framework is a powerful tool for developing and executing exploit code against a remote target machine. It provides a complete environment for penetration testing and exploit development.",
            importance = "Essential tool for security professionals to test system vulnerabilities and develop security solutions.",
            runCommand = "msfconsole",
            guiPath = "Kali Linux icon → Exploitation Tools → Metasploit Framework",
            usage = "Launch msfconsole from terminal. Use 'search' to find exploits, 'use' to select an exploit module, 'set' to configure options, and 'exploit' to run the attack.",
            note = "Always ensure you have authorization before testing on any system. Unauthorized access is illegal.",
            category = "Exploitation"
        ),
        KaliTool(
            id = 3,
            name = "Nmap",
            description = "Network scanner",
            mainFunction = "Nmap is a free and open-source network scanner used to discover hosts and services on a computer network by sending packets and analyzing the responses.",
            importance = "One of the most popular tools for network discovery and security auditing.",
            runCommand = "nmap [target]",
            guiPath = "Kali Linux icon → Information Gathering → Nmap",
            usage = "Basic scan: 'nmap 192.168.1.1'\nPort scan: 'nmap -p 1-1000 192.168.1.1'\nOS detection: 'nmap -O 192.168.1.1'\nService version: 'nmap -sV 192.168.1.1'",
            note = "Scanning networks without permission is illegal. Only scan systems you own or have explicit permission to test.",
            category = "Information Gathering"
        ),
        KaliTool(
            id = 4,
            name = "Hydra",
            description = "Password cracking tool",
            mainFunction = "Hydra is a parallelized login cracker which supports numerous protocols to attack. It is very fast and flexible, making it a favorite tool for penetration testers.",
            importance = "Useful for testing password strength and security of authentication systems.",
            runCommand = "hydra -l [username] -P [passwordlist] [target] [protocol]",
            guiPath = "Kali Linux icon → Password Attacks → Hydra",
            usage = "SSH attack: 'hydra -l root -P passwords.txt ssh://192.168.1.1'\nFTP attack: 'hydra -l admin -P rockyou.txt ftp://192.168.1.1'",
            note = "Only use on systems you own or have authorization to test. Unauthorized password cracking is illegal.",
            category = "Password Attacks"
        ),
        KaliTool(
            id = 5,
            name = "Crunch",
            description = "Wordlist generator",
            mainFunction = "Crunch can create a wordlist based on criteria you specify. The output can be sent to the screen, file, or to another program.",
            importance = "Essential for creating custom password lists for penetration testing.",
            runCommand = "crunch [min] [max] [charset] -o [output_file]",
            guiPath = "Terminal only",
            usage = "Generate 4-6 digit numbers: 'crunch 4 6 0123456789 -o numlist.txt'\nGenerate with pattern: 'crunch 8 8 -t pass%%%% -o passwords.txt'",
            note = "Large wordlists can consume significant disk space. Use filters to optimize.",
            category = "Password Attacks"
        ),
        KaliTool(
            id = 6,
            name = "Dmitry",
            description = "Information gathering tool",
            mainFunction = "DMitry (Deepmagic Information Gathering Tool) is a UNIX command line application coded in C. It can gather as much information as possible about a host.",
            importance = "Useful for reconnaissance and initial information gathering phase.",
            runCommand = "dmitry -winsepo [output_file] [target]",
            guiPath = "Terminal only",
            usage = "Full scan: 'dmitry -winsepo result.txt example.com'\nOptions: -w (whois), -i (IP), -n (netcraft), -s (subdomains), -e (email), -p (port scan)",
            note = "Information gathering should be done ethically and legally.",
            category = "Information Gathering"
        ),
        KaliTool(
            id = 7,
            name = "Hash-Identifier",
            description = "Hash type identifier",
            mainFunction = "Hash Identifier is a tool that can identify different types of hashes used to encrypt data and especially passwords.",
            importance = "Helpful for identifying hash types before attempting to crack them.",
            runCommand = "hash-identifier",
            guiPath = "Terminal only",
            usage = "Run 'hash-identifier' and paste the hash when prompted. The tool will identify possible hash types.",
            note = "Knowing the hash type is crucial for efficient password cracking.",
            category = "Password Attacks"
        ),
        KaliTool(
            id = 8,
            name = "Wafw00f",
            description = "Web Application Firewall detector",
            mainFunction = "WAFW00F identifies and fingerprints Web Application Firewall (WAF) products protecting a website.",
            importance = "Important for web application penetration testing to identify security measures.",
            runCommand = "wafw00f [target_url]",
            guiPath = "Terminal only",
            usage = "Basic scan: 'wafw00f https://example.com'\nVerbose mode: 'wafw00f -v https://example.com'",
            note = "Helps understand what security measures are in place before testing.",
            category = "Web Application Analysis"
        ),
        KaliTool(
            id = 9,
            name = "SpiderFoot",
            description = "OSINT automation tool",
            mainFunction = "SpiderFoot is an open-source intelligence automation tool. It integrates with just about every data source available and uses various methods for data analysis.",
            importance = "Excellent for automated reconnaissance and intelligence gathering.",
            runCommand = "spiderfoot -l 127.0.0.1:5001",
            guiPath = "Kali Linux icon → Information Gathering → SpiderFoot",
            usage = "Start web interface with 'spiderfoot -l 127.0.0.1:5001', then open browser to http://127.0.0.1:5001",
            note = "Very powerful tool that can gather extensive information. Use responsibly.",
            category = "Information Gathering"
        ),
        KaliTool(
            id = 10,
            name = "Pdfid",
            description = "PDF analysis tool",
            mainFunction = "PDFiD scans a PDF file to look for certain PDF keywords, allowing you to identify PDF documents that contain (or are likely to contain) JavaScript or execute an action when opened.",
            importance = "Useful for analyzing suspicious PDF files for malware.",
            runCommand = "pdfid [pdf_file]",
            guiPath = "Terminal only",
            usage = "Analyze PDF: 'pdfid suspicious.pdf'\nGet detailed info: 'pdfid -e suspicious.pdf'",
            note = "Essential for malware analysis and forensics work.",
            category = "Forensics"
        )
    )
}

