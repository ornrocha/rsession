package org.math.R;

import org.jvnet.winp.WinProcess;

public class WinRserveProcFollower {
	
	
	private Integer pid;
	
	public WinRserveProcFollower() {}
	
	
	
	public void followProcess(Process p) {
		this.pid=new WinProcess(p).getPid();
	}
	
	
	public void killProcessRecursively() {
		if(pid!=null)
			new WinProcess(pid).killRecursively();
	}
	
	
	public void killProcess() {
		if(pid!=null)
			new WinProcess(pid).kill();
	}

}
