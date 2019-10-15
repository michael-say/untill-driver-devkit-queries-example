package com.untill.drivers.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.untill.driver.IDriver;
import com.untill.driver.IDriverContext;
import com.untill.driver.ProgressQueryType;
import com.untill.driver.interfaces.DriverInterfaces;
import com.untill.driver.interfaces.IDriverInterface;
import com.untill.driver.interfaces.eft.EftRequest;
import com.untill.driver.interfaces.eft.EftResult;
import com.untill.driver.interfaces.eft.EftSettings;
import com.untill.driver.interfaces.eft.EftTransactionResult;
import com.untill.driver.interfaces.eft.IEft;
import com.untill.driver.params.DriverConfiguration;
import com.untill.driver.params.DriverParam;

public class TestQueryDriver implements IDriver, IEft {
	
	public static class EUserCancelled extends Exception {
		private static final long serialVersionUID = -5259017767394809441L;
	};
	
	public static interface IMyRunnable {
		/**
		 * @return True to continue
		 */
		boolean run();
	} 

	public static final String PARAM_URL = "url";

	public static final String PARAM_URL_DEFAULT = "https://127.0.0.1:7777";

	public static final String PARAM_USER = "user";

	public static final String PARAM_PASSWORD = "password";

	@Override
	public ArrayList<DriverParam> getParamsList() {
		return DriverParam.list();
	}

	@Override
	public String getDriverName() {
		return "Test Queries Example";
	}

	@Override
	public String getProviderName() {
		return "unTill";
	}

	@Override
	public String getVersion() {
		return this.getClass().getPackage().getImplementationVersion();
	}

	@Override
	public Map<Class<? extends IDriverInterface>, IDriverInterface> init(IDriverContext context) {
		this.ctx = context;
		return DriverInterfaces.map(IEft.class, this);
	}

	@Override
	public void finit() {
	}
	
	IDriverContext ctx;
	
	@Override
	public EftSettings getEftSettings() {		
		return new EftSettings.Builder().setCancellingByWaiterSupported(true).build();
	}
	
	private void cancelableTask(int seconds, String guid, IMyRunnable r) throws EUserCancelled {
		long t0 = System.currentTimeMillis();
		while (System.currentTimeMillis() < t0 + seconds * 1000) {
			try {
				Thread.sleep(100);
				if (ctx.getProgress().isCancelRequested(guid)) {
					ctx.getProgress().showProgressMessage(guid, "Cancel requested");
					Thread.sleep(1000); // simulating some job
					throw new EUserCancelled();
				}
				if (!r.run())
					break;
			} catch (InterruptedException e) {
				break;
			}			
		}		
	}
	
	private void someJob(int seconds, String guid) throws EUserCancelled {
		cancelableTask(seconds, guid, () -> {
			return true; // do nothing, just wait
		} );
	}
	
	private String waitForInput(int seconds, String guid, int query) throws EUserCancelled {
		cancelableTask(seconds, guid, () -> {
			String res = ctx.getProgress().getQueryResult(guid, query);
			if (res != null) {
				queryResults.get(guid).put(query, res);
				return false; // stop waiting
			} else
				return true; // continue waiting
		} );
		return queryResults.get(guid).get(query);
	}

	static final int QUERY_ARE_YOU_SURE = 1;
	
	private Map<String, Map<Integer, String>> queryResults = new HashMap<>();
	
	@Override
	public EftResult operation(DriverConfiguration cfg, EftRequest request) {
		String guid = request.getGuid();
		queryResults.put(guid, new HashMap<>());
		try {
			ctx.getProgress().showProgressMessage(guid, "Request started");
			someJob(3, guid);  
			ctx.getProgress().showProgressMessage(guid, "Request still running");
			someJob(3, guid);  
			ctx.getProgress().showQuery(guid, QUERY_ARE_YOU_SURE, ProgressQueryType.YES_NO, "Are you a sure?");
			String answer = waitForInput(20, guid, QUERY_ARE_YOU_SURE);
			if ("y".equals(answer)) {
				ctx.getProgress().showProgressMessage(guid, "Completing request");
				someJob(6, guid);  
				return new EftResult.Builder().setTransactionResult(EftTransactionResult.SUCCESS).build();
			} else 			
				return new EftResult.Builder().setTransactionResult(EftTransactionResult.DECLINED).build();			
		} catch (EUserCancelled e) {
			return new EftResult.Builder().setTransactionResult(EftTransactionResult.CANCELLED).build();
		}
	}

}
