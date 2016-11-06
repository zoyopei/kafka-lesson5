package yoyo;

import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

public class LeaderElection {
	ZooKeeper zk;
	String host;
	String path;
	String name;
	String myname;
	Integer mynumber;
	String beforname;
	Boolean isLeader = false;
	
	public LeaderElection(String host, String path, String name) {
		this.host = host;
		this.path = path;
		this.name = name;
	}
	
	public void Connect(){
		try {
			zk = new ZooKeeper(host, 6000, null);
			myname = zk.create(path + name, null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
			System.out.println("my name: " + myname);
			mynumber = Integer.valueOf(myname.substring(path.length() + name.length()));
			System.out.println("my number" + mynumber);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public Boolean IsLeader() {
		try {
			beforname = "";
			Integer befornumber = 0;
			List<String> nodes = zk.getChildren("/test", false);
			for(String n : nodes) {
				Integer num = Integer.valueOf(n.substring(name.length()));
				System.out.println("get node " + n);
				if(num < mynumber) {
					if(num > befornumber) {
						befornumber = num;
						beforname = path + n;
					}
				}
			}
			
			if(beforname.length() == 0) {
				isLeader = true;
				System.out.println("I am leader " + System.currentTimeMillis());
				return true;
			}
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public void MonitorBefor() {
		try {
			if(IsLeader()) {
				return ;
			}
			
			System.out.println("I am not leader, wait..." + System.currentTimeMillis());
			zk.exists(beforname, new Watcher() {
				public void process(WatchedEvent event) {
					System.out.println("event: " + event.getPath() + " " + event.getType().name());
					MonitorBefor();
				}
			});
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Boolean MonitorBeforAsync(final LeaderCallBack lcb) {
		try {
			if(IsLeader()) {
				lcb.process();
				return true;
			}
			
			System.out.println("I am not leader, wait... " + System.currentTimeMillis());
			zk.exists(beforname, new Watcher() {
				public void process(WatchedEvent event) {
					System.out.println("event: " + event.getPath() + " " + event.getType().name());
					MonitorBeforAsync(lcb);
				}
			});
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public void LeaderBlock() {
		MonitorBefor();
		while (isLeader == false) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public Boolean LeaderNonBlock(LeaderCallBack lcb) {
		return MonitorBeforAsync(lcb);
	}
	
	public static void main(String[] args) {
		LeaderElection le1 = new LeaderElection("zookeeper0:2181", "/test/", "leader");
		le1.Connect();
//		le1.LeaderBlock();
		le1.LeaderNonBlock(new LeaderCallBack() {
			@Override
			public void process() {
				System.out.println("call back I am leader now " + System.currentTimeMillis());
			}
		});
		try {
			Thread.sleep(100000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
