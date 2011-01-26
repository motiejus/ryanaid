### Rynanair crawler that uses Webkit as backend ###

Works as of 2011-01-08. Then I found Azuon, which
made my work unnecessary.
Beware, this thing does not show good coding practices. It is more a working prototype.

### Usage ###

## For single instance ##

ryanaid.jar is a java runnable. You will need some kind of JRE.
It takes environment variables as parameters:
date_from="2010-03-11" # Start checking at this date
check_days=90 # Finish at ~ may 11'th
from="KUN" # Airport code, Kaunas in this case
to="EDI" # Edinburgh
wait_ms=5000 # Wait this number of ms after every request (click). Useful if you do not want to be banned from the ryanair website.
socks_host=127.0.0.1 # Optional. Connect through this place
socks_port=1080 # Self explanatory if you know what SOCKS is

All variables have default values, so you can just launch it like this:

$ from="KUN" to="HHN" java -jar ryanaid.jar

You may have firebug installed. Check source for details.

## For multiple firefox instances (real automatic crawling) ##

Here are helpers that help to crawl all the ryanair. You will need:
* (da|a|ba|c)sh (a _shell_)
* autossh (for reliable socks proxies)
* crawler_key and crawler_key.pub, which are rsa/dsa ssh key pairs (to connect to your reliable socks-via-ssh proxies)
Basically your crawling speed is limited by how many hosts and RAM you have.
9 crawlers were working full-time, used ~2.5 GiB RAM and ~150kBps bandwidth as average (90% - download).
Fetched all ryanair flights (now-3 months) approximately once a day.

$ ssh-keygen -t rsa <ENTER><ENTER>
$ mv ~/.ssh/id_rsa crawler_key
$ mv ~/.ssh/id_rsa.pub crawler_key.pub

$ ./download_dests > dests.txt # Renew destination pairs
## Fill all the hosts you can get access to to the tunnel_hosts.txt. Example:
127.0.0.1:22
srv.example.com:22
wrt.example.com:2222
## Upload your key to host so they you connect using your private key (http://www.google.com/search?q=authorized_keys)
$ ./go_1crawler 1 &
$ ./go_1crawler 2 &
$ ./go_1crawler 3 &
and so on, repeat for each host you have in your tunnel_hosts.txt.

actiondir/ is logs and pids of autossh
datadir/ is the thing you want to process.

Sorry for writing style. Patches for this README are very welcome :)
