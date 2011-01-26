Rynanair crawler that uses WebKit as backend
============================================

Usage for single instance
-------------------

You will need:

1. X server (vncserver is fine)

2. JRE (tested with openjdk6 and sun-jre-1.5)

3. Firefox (tested with 3.5)

Launchable is ryanaid.jar. It takes environment variables as parameters:
    # Start checking at this date
    date_from="2010-03-11"

    # Finish at ~ may 11'th
    check_days=90 

    # Airport code, Kaunas in this case
    from="KUN"

    # Edinburgh
    to="EDI"

    # Wait this number of ms after every request (click). Useful if you do not want to be banned from the ryanair website.
    wait_ms=5000

    # Optional. Connect through this place
    socks_host=127.0.0.1

    # Self explanatory if you know what SOCKS is here for
    socks_port=1080

All variables have default values, so you can just launch it like this:

    $ from="KUN" to="HHN" java -jar ryanaid.jar

You may have firebug installed. Check source for details.

Usage for parallel instances (real automated crawling)
------------------------------------------------------

Here are helpers that help to crawl all the ryanair flights. You will need:

1. (da|a|ba)sh (a _shell_)

2. autossh (for reliable socks proxies)

3. crawler_key and crawler_key.pub, which are rsa/dsa ssh key pairs (to connect to your reliable socks-via-ssh proxies)

Basically your crawling speed is limited by how many hosts and RAM you have.
9 crawlers were working full-time, used overall ~2.5 GiB RAM and ~150kBps bandwidth as average (95% of which was download).
I was fetching all ryanair flights in interval [now-3 months] once in two days.

    $ ssh-keygen -t rsa <ENTER><ENTER>
    $ mv ~/.ssh/id_rsa crawler_key
    $ mv ~/.ssh/id_rsa.pub crawler_key.pub
    $ python download_dests > dests.txt # Renew destination pairs

Fill all the hosts you can get access to to the tunnel_hosts.txt. Example:

    motiejus@127.0.0.1:22
    webadmin@srv.example.com:22
    root@openwrt.example.com:2222

Upload your key to hosts so you can connect using your private key.

    $ echo no-pty,no-x11-forwarding,no-agent-forwarding,command="/bin/true" $(cat crawler_key.pub) | \
        cat >> $HOME/.ssh/authorized_keys
    $ echo no-pty,no-x11-forwarding,no-agent-forwarding,command="/bin/true" $(cat crawler_key.pub) | \
        ssh webadmin@srv.example.com "cat >> .ssh/authorized_keys"
    $ echo no-pty,no-x11-forwarding,no-agent-forwarding,command="/bin/true" $(cat crawler_key.pub) | \
        ssh root@openwrt.example.com "cat >> /etc/dropbear/authorized_keys"

Because of the options specified in the authorized_key, you cannot get shell access in remote server.
This might be an advantage when persuading your friends to give ssh access to their boxes.

Then is the fun part:
    $ sh ./go_1crawler.sh 1 &
    $ sh ./go_1crawler.sh 2 &
    $ sh ./go_1crawler.sh 3 &
and so on, repeat for each host you have in your tunnel_hosts.txt.

actiondir/ is logs and pids of autossh

datadir/ is the directory you want to look at (results).

Example output
-------------
    motiejus@aviastopas:~/crawl$ head -10 datadir/2011-01-09_21/KUN-STN_2011-01-09+21:50:56-5.flights
    KUN-STN|2011-01-12 12:00|2011-01-12 12:40|604.71 LTL|2|2011-01-09 21:51:34
    KUN-STN|2011-01-13 12:00|2011-01-13 12:40|604.71 LTL|2|2011-01-09 21:51:39
    KUN-STN|2011-01-14 12:00|2011-01-14 12:40|504.71 LTL|1|2011-01-09 21:51:46
    KUN-STN|2011-01-15 12:00|2011-01-15 12:40|414.71 LTL|1|2011-01-09 21:51:52
    KUN-STN|2011-01-16 12:00|2011-01-16 12:40|414.71 LTL|1|2011-01-09 21:52:03
    KUN-STN|2011-01-17 12:00|2011-01-17 12:40|344.71 LTL|0|2011-01-09 21:52:10
    KUN-STN|2011-01-18 12:00|2011-01-18 12:40|344.71 LTL|0|2011-01-09 21:52:16
    KUN-STN|2011-01-19 12:00|2011-01-19 12:40|344.71 LTL|0|2011-01-09 21:52:21
    KUN-STN|2011-01-20 12:00|2011-01-20 12:40|344.71 LTL|0|2011-01-09 21:52:26
    KUN-STN|2011-01-21 12:00|2011-01-21 12:40|344.71 LTL|0|2011-01-09 21:52:33

Output format:
    From-To|Depart|Arrive|Price|Seats left (0 for ∞)|Date crawled

Notes
-----------
Works as of 2011-01-12. Then I found Azuon, which made my work unnecessary. Then I paid them 5€ and stopped crawling myself.
The code is not as good as it I would like it to be, since it is more a working prototype rather than production-ready-finished-thing.
However, it is quite well tested.

Comments and patches welcome. Have fun!
