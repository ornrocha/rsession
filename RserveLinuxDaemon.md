## Few steps to enable Rserve daemon at startup of your Linux server##

* install R from http://cran.r-project.org (tested with 2.9)
* install Rserve as user:
  * get Rserve from rforge: wget http://www.rforge.net/src/contrib/Rserve_0.6-0.tar.gz
  * install and compile it (need r-base-dev, not only r-base installed) R CMD INSTALL Rserve_0.6-0.tar.gz 
* create Rserve.sh startup script in /home/user/R/x86_64-pc-linux-gnu-library/*/Rserve:
```bash
    #!/bin/bash
    /usr/bin/R CMD Rserve --vanilla --RS-conf Rserve.conf
```
* check that Rserve.sh is executable, try to launch it and verify Rserve is available (ps -u user | grep Rserve should return something, then kill Rserve)
* create Rserve.conf file in /home/user/R/x86_64-pc-linux-gnu-library/*/Rserve
```
    workdir tmp
    remote enable
    fileio enable
```
* create as root /etc/init.d/Rserved
```bash
    #!/bin/bash

    echo " * Launching Rserve daemon ..."

    RSERVE_HOME=/home/user/R/x86_64-pc-linux-gnu-library/2.9/Rserve

    start-stop-daemon --start --chdir $RSERVE_HOME --chuid user --exec $RSERVE_HOME/Rserve.sh > /var/log/Rserve.log 2>&1 &
    echo " * Rserve daemon running ..."

    exit 0
```
* check that Rserved is executable, try to launch it and verify Rserve is available (ps -u user | grep Rserve should return something, then kill Rserve)
* link Rserved in /etc/rc2.d (for ubuntu, maybe other /etc/rc.d for others linux distribution): as root cd /etc/rc2.d; ln -s ../init.d/Rserved S99Rserved
* reboot your server to verify Rserve was correclty launched at startup. 

Now, you can use this linux server as a backend computing engine for your Java applications on desktop.
```java
Rsession s = Rsession.newRemoteInstance(System.out,RserverConf.parse("R://myLinuxServer"));
HashMap<String,Object> vars = new HashMap<String,Object>();
vars.put("a",1.0);
vars.put("b",1.0);
double[] rand = (double[]) s.eval("rnorm(10,a,b)",vars);
```
