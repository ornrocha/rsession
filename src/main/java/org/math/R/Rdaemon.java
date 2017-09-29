package org.math.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.math.R.Logger.Level;
import org.rosuda.REngine.Rserve.RConnection;

import com.github.sarxos.winreg.HKey;
import com.github.sarxos.winreg.RegistryException;
import com.github.sarxos.winreg.WindowsRegistry;
import com.vdurmont.semver4j.Semver;


/**
 *
 * @author richet
 * 
 * 
 * Features added/changed by ornrocha
 * 
 */
public class Rdaemon {
    
    RserverConf conf;
    Process process;
    private final Logger log;
    static File APP_DIR = new File(System.getProperty("user.home") + File.separator + ".Rserve");
    public String R_HOME = null;
    public String R_BIN_PATH = null;
    String R_USER_LIBS=null;
    
    public static String Bit64="b64";
	public static String Bit32="b32";
	private WinRserveProcFollower pfollow;
    
    static {
        boolean app_dir_ok = false;
        if (!APP_DIR.exists()) {
            app_dir_ok = APP_DIR.mkdir();
        } else {
            app_dir_ok = APP_DIR.isDirectory() && APP_DIR.canWrite();
        }
        if (!app_dir_ok) {
            System.err.println("Cannot write in " + APP_DIR.getAbsolutePath());
        }
    }
    
    /*public Rdaemon(RserverConf conf, Logger log, String R_HOME) {
        this.conf = conf;
        this.log = log != null ? log : new Slf4jLogger();
        findR_HOME(R_HOME);
        log.println("Environment variables:\n  " + R_HOME_KEY + "=" + Rdaemon.R_HOME + "\n  " + Rserve_HOME_KEY + "=" + Rdaemon.Rserve_HOME, Level.INFO);        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                _stop();
            }
        });
    }*/
    
    private Rdaemon(){
    	this.log = new Slf4jLogger();
    	
    }
    
    
    public Rdaemon(RserverConf conf, Logger log, String R_HOME, String R_USER_LIBS) {
        this.conf = conf;
        this.log = log != null ? log : new Slf4jLogger();
        findR_HOME(R_HOME);
        this.R_USER_LIBS=R_USER_LIBS;
       // log.println("Environment variables:\n  " + R_HOME_KEY + "=" + Rdaemon.R_HOME /*+ "\n  " + Rserve_HOME_KEY + "=" + Rdaemon.Rserve_HOME*/, Level.INFO);
        log.println("R bin location:\n  " + R_HOME_KEY + "=" + R_BIN_PATH /*+ "\n  " + Rserve_HOME_KEY + "=" + Rdaemon.Rserve_HOME*/, Level.INFO);  
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                _stop();
            }
        });
    }
    

    private void _stop() {
        stop();
    }
    
    public Rdaemon(RserverConf conf, Logger log) {
        this(conf, log, null);
    }
    
    public Rdaemon(RserverConf conf, Logger log, String R_HOME) {
        this(conf, log, R_HOME,null);
    }
    
    
    public final static String R_HOME_KEY = "R_HOME";
    
    
    public static String getBinaryPathToR(){
    	Rdaemon d=new Rdaemon();
    	return d.findR_HOME(null).R_BIN_PATH;
    }
    
    
    public Rdaemon findR_HOME(String r_HOME) {
        Map<String, String> env = System.getenv();
        Properties prop = System.getProperties();
        
        if (r_HOME!=null) R_HOME = r_HOME;
        
        if (R_HOME == null || !(new File(R_HOME).isDirectory())) {
            
        	if (env.containsKey(R_HOME_KEY)) {
                R_HOME = env.get(R_HOME_KEY);
            }
            
            if (R_HOME == null || prop.containsKey(R_HOME_KEY) || !(new File(R_HOME).isDirectory())) {
                R_HOME = prop.getProperty(R_HOME_KEY);
            } 
            
            if (R_HOME == null || !(new File(R_HOME).isDirectory())) {
                R_HOME = "R";
            }
            
            if (R_HOME == null || !(new File(R_HOME).isDirectory())) {
  
                if (System.getProperty("os.name").contains("Win")) {
                    /*for (int major = 20; major >= 0; major--) {
                        //int major = 10;//known to work with R 2.9 only.
                        if (R_HOME == null) {
                            for (int minor = 10; minor >= 0; minor--) {
                                //int minor = 0;
                                r_HOME = "C:\\Program Files\\R\\R-3." + major + "." + minor + "\\";
                                if (new File(r_HOME).exists()) {
                                    R_HOME = r_HOME;
                                    break;
                                }
                            }
                        } else {
                            break;
                        }
                    }*/
                	
                	String winRdir=getWindowRPathFromRegistry();
                	if(winRdir!=null){
                		String minversionnumber="3.0.0";
                		String currversionnumber=filterVersionNumber(winRdir);
                		
                		Semver minversion = new Semver(minversionnumber);
                		Semver currversion = new Semver(currversionnumber);
                		
                		if(currversion.isGreaterThan(minversion) && new File(winRdir).exists()){
                			 R_HOME = winRdir;
                			 R_BIN_PATH=Rsession.validatePath(R_HOME+ File.separator + "bin" + File.separator + "R");
                		}
                		else{
                			System.err.println("Please install  a version of R greater than 3.0.0");
                		}
                	}
                	else
                		System.err.println("R path is unavailable in windows registry, please check if you have R environment installed in your computer");
                	
                	
                		
                } else {
                    try {
						R_BIN_PATH =findArgumentInLinux(false,"which","R");
						if(new File(R_BIN_PATH).exists()){
							String Rtmphome=FilenameUtils.getFullPathNoEndSeparator(R_BIN_PATH);
							R_HOME=FilenameUtils.getFullPathNoEndSeparator(Rtmphome);
						}
						//  return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
                }
            }
        }
        
        /*if (R_HOME == null) {
            return false;
        }
        
        return new File(R_HOME).isDirectory();*/
        
        return this;
    }
    
    
 
    public static String getWindowRPathFromRegistry(){
    	
    	//WindowsRegistry regwin=WindowsRegistry.getInstance();
    	
    	String basetree="SOFTWARE\\R-core\\R";
    	
    	String tree=basetree;
    	String bitFlag=getOperationSystemBitFlag();
    	if(bitFlag!=null && bitFlag.equals(Bit64))
    		tree="SOFTWARE\\R-core\\R64";

    	try {
    		String Rpath=getRegistryValue(tree);
    		if(Rpath==null)
    			Rpath=getRegistryValue(basetree);

    		if(Rpath!=null){
    			//Rpath=Rpath.replace("\\", File.separator)+File.separator+"bin"+File.separator+"R";
    			return Rpath.replace("\\", File.separator);
    		}
    		
		} catch (RegistryException e) {
			new Slf4jLogger().println("R path is unavailable in windows registry", Level.ERROR);
		}
    	return null;
    }
    
    private static String getRegistryValue(String treepath) throws RegistryException{
    	WindowsRegistry regwin=WindowsRegistry.getInstance();
    	
    	List<String> keys = regwin.readStringSubKeys(HKey.HKLM,treepath);
    	String rversion=null;
    	if(keys!=null){
    		if(keys.size()>1)
    			rversion=getHightVersion(keys);
    		else
    			rversion=keys.get(0);
    	}

    	if(rversion!=null){
    		treepath=treepath+"\\"+rversion;
    		String value = regwin.readString(HKey.HKLM, treepath, "InstallPath");
    		//log.println("stopping R daemon... " + conf, Level.INFO);

    		if(value!=null)
    			return value;
    	}
    	return null;
    }
    
    
	private static String getOperationSystemBitFlag(){
	  	
		String arch = SystemUtils.OS_ARCH;

		if (arch.equals("amd64") || arch.equals("x86_64")) {
			return Bit64;
		}    
		else if (arch.equals("x86") || arch.equals("i386")) {
			return Bit32;
		}
		else 
			return null;
	}
	
	 private static String getHightVersion(List<String> keys){
	    	String version=keys.get(0);

			Semver currversion = new Semver(filterVersionNumber(version));
	    	
	    	for (int i = 1; i < keys.size(); i++) {
	    		Semver checkversion =new Semver(filterVersionNumber(keys.get(i)));
				if(checkversion.isGreaterThan(currversion)){
				    version=keys.get(i);
				    currversion=checkversion;
				}
			}
	    	
	    	return version;
	    }
	    
	 
	 private static String filterVersionNumber(String version){
	    	Pattern pat=Pattern.compile("(\\d+(.\\d+)*)");
	    	Matcher m=pat.matcher(version);
	    	if(m.find()){
	    		String vers=m.group(1);
	    		return vers;
	    	}
	    	return version;
	    }
	 
	 
	 public static String findArgumentInLinux(boolean checkerrors, String...cmds) throws Exception{
	    	ProcessBuilder build= new ProcessBuilder(cmds);
	    	if(checkerrors)
	    		build.redirectErrorStream(true);
	    	Process p =build.start();
	    	
	    	SimpleCmdChecker checker=new SimpleCmdChecker(p.getInputStream());
	    	Thread stdout=new Thread(checker);
	    	stdout.run();
	    	return checker.getOutput();
	    }
    

    /*public static boolean findRserve_HOME(String path) {
    Map<String, String> env = System.getenv();
    Properties prop = System.getProperties();
    
    Rserve_HOME = path;
    if (Rserve_HOME == null || !(new File(Rserve_HOME).exists()) || !new File(Rserve_HOME).getName().equals("Rserve")) {
    if (env.containsKey(Rserve_HOME_KEY)) {
    Rserve_HOME = env.get(Rserve_HOME_KEY);
    }
    
    if (Rserve_HOME == null || prop.containsKey(Rserve_HOME_KEY) || !(new File(Rserve_HOME).exists()) || !new File(Rserve_HOME).getName().equals("Rserve")) {
    Rserve_HOME = prop.getProperty(Rserve_HOME_KEY);
    }
    
    if (Rserve_HOME == null || !(new File(Rserve_HOME).exists()) || !new File(Rserve_HOME).getName().equals("Rserve")) {
    Rserve_HOME = null;
    String OS_NAME = prop.getProperty("os.name");
    String OS_ARCH = prop.getProperty("os.arch");
    if (OS_ARCH.equals("amd64")) {
    OS_ARCH = "x86_64";
    }
    if (OS_ARCH.endsWith("86")) {
    OS_ARCH = "x86";
    }
    
    if (OS_NAME.contains("Windows")) {
    Rserve_HOME = "lib\\Windows\\" + OS_ARCH + "\\Rserve\\";
    } else if (OS_NAME.equals("Mac OS X")) {
    Rserve_HOME = "lib/MacOSX/" + OS_ARCH + "/Rserve";
    } else if (OS_NAME.equals("Linux")) {
    Rserve_HOME = "lib/Linux/" + OS_ARCH + "/Rserve";
    } else {
    System.err.println("OS " + OS_NAME + "/" + OS_ARCH + " not supported for automated RServe finding.");
    }
    
    if (!new File(Rserve_HOME).exists()) {
    System.err.println("Unable to find Rserve in " + Rserve_HOME);
    Rserve_HOME = null;
    } else {
    Rserve_HOME = new File(Rserve_HOME).getPath().replace("\\", "\\\\");
    }
    }
    }
    
    if (Rserve_HOME != null && new File(Rserve_HOME).exists()) {
    setRecursiveExecutable(new File(Rserve_HOME));
    return true;
    } else {
    return false;
    }
    }*/
    static void setRecursiveExecutable(File path) {
        for (File f : path.listFiles()) {
            if (f.isDirectory()) {
                f.setExecutable(true);
                setRecursiveExecutable(f);
            } else if (!f.canExecute() && (f.getName().endsWith(".so") || f.getName().endsWith(".dll"))) {
                f.setExecutable(true);
            }
        }
        
    }
    
    public void stop() {
        log.println("stopping R daemon... " + conf, Level.INFO);
        if (!conf.isLocal()) {
            throw new UnsupportedOperationException("Not authorized to stop a remote R daemon: " + conf.toString());
        }
        
        try {
            RConnection s = conf.connection;//connect();
            if (s == null || !s.isConnected()) {
            	log.println("R daemon already stoped.", Level.INFO);
                return;
            }
            s.shutdown();
            
            if(pfollow!=null)
            	pfollow.killProcessRecursively();
            
        } catch (Exception ex) {
        	log.println(ex.getMessage(), Level.ERROR);
        }

        log.println("R daemon stoped.", Level.INFO);
    }
    
    public void start(String http_proxy) {
        if (R_BIN_PATH == null && (R_HOME == null || !(new File(R_HOME).exists()))) {
            throw new IllegalArgumentException("R_HOME environment variable not correctly set.\nYou can set it using 'java ... -D" + R_HOME_KEY + "=[Path to R] ...' startup command.");
        }
        
        if (!conf.isLocal()) {
            throw new UnsupportedOperationException("Unable to start a remote R daemon: " + conf.toString());
        }

        /*if (Rserve_HOME == null || !(new File(Rserve_HOME).exists())) {
        throw new IllegalArgumentException("Rserve_HOME environment variable not correctly set.\nYou can set it using 'java ... -D" + Rserve_HOME_KEY + "=[Path to Rserve] ...' startup command.");
        }*/
        
        log.println("checking Rserve is available... ", Level.INFO);
        String rcmd=null;
        
        if(R_BIN_PATH!=null)
        	rcmd=R_BIN_PATH+ (System.getProperty("os.name").contains("Win") ? ".exe" : "");
        else if(R_BIN_PATH==null && R_HOME!=null)
        	rcmd=R_HOME + File.separator + "bin" + File.separator + "R" + (System.getProperty("os.name").contains("Win") ? ".exe" : "");
       // String rcmd=R_HOME + File.separator + "bin" + File.separator + "R" + (System.getProperty("os.name").contains("Win") ? ".exe" : "");
       // boolean RserveInstalled = StartRserve.isRserveInstalled(R_HOME + File.separator + "bin" + File.separator + "R" + (System.getProperty("os.name").contains("Win") ? ".exe" : ""));
        
        boolean RserveInstalled = false;
        if(R_USER_LIBS!=null)
        	RserveInstalled=StartRserve.isRserveInstalled(rcmd,R_USER_LIBS);
        else
        	RserveInstalled=StartRserve.isRserveInstalled(rcmd);
        

        if (!RserveInstalled) {
        	log.println("  no", Level.INFO);
            RserveInstalled = StartRserve.installRserve(rcmd, http_proxy, null,R_USER_LIBS);
            
           // RserveInstalled = StartRserve.installRserve(R_HOME + File.separator + "bin" + File.separator + "R" + (System.getProperty("os.name").contains("Win") ? ".exe" : ""), http_proxy, null,R_USER_LIBS);
            if (RserveInstalled) {
            	log.println("  ok", Level.INFO);
            } else {
            	log.println("  failed.", Level.ERROR);
                String notice = "Please install Rserve manually in your R environment using \"install.packages('Rserve')\" command.";
                log.println(notice, Level.ERROR);
                System.err.println(notice);
                return;
            }
        } else {
        	log.println("  ok", Level.INFO);
        }
        
        log.println("starting R daemon... " + conf, Level.INFO);
        
        StringBuffer RserveArgs = new StringBuffer("--no-save --slave");
        if (conf.port > 0) {
            RserveArgs.append(" --RS-port " + conf.port);
        }
        
        if (System.getProperty("os.name").contains("Win"))
        	pfollow=new WinRserveProcFollower();
        //boolean started = StartRserve.launchRserve(R_HOME + File.separator + "bin" + File.separator + "R" + (System.getProperty("os.name").contains("Win") ? ".exe" : ""), /*Rserve_HOME + "\\\\..", */ "--no-save --slave", RserveArgs.toString(), false, R_USER_LIBS);
        boolean started = StartRserve.launchRserve(rcmd, /*Rserve_HOME + "\\\\..", */ "--no-save --slave", RserveArgs.toString(), false, R_USER_LIBS,pfollow);
        
        if (started) {
        	log.println("  ok", Level.INFO);
        } else {
        	log.println("  failed", Level.ERROR);
        }
    }
    
  /*  public boolean installPackageLocallyViaCommandLine(String packagename, String repository){
    	 String rcmd=null;
         
         if(R_BIN_PATH!=null)
         	rcmd=R_BIN_PATH+ (System.getProperty("os.name").contains("Win") ? ".exe" : "");
         else if(R_BIN_PATH==null && R_HOME!=null)
         	rcmd=R_HOME + File.separator + "bin" + File.separator + "R" + (System.getProperty("os.name").contains("Win") ? ".exe" : "");
         
         return StartRserve.installPackageCommandLine(packagename, rcmd, null, repository, R_USER_LIBS);
    }*/
    
    
    public static String timeDigest() {
        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        StringBuffer sb = new StringBuffer();
        sb =
        sdf.format(new Date(time), sb, new java.text.FieldPosition(0));
        return sb.toString();
    }
    
    public static void main(String[] args) throws InterruptedException {
        Rdaemon d = new Rdaemon(new RserverConf(null, -1, null, null, null), new Slf4jLogger());
        d.start(null);
        Thread.sleep(2000);
        d.stop();
        Thread.sleep(2000);
    }
}
