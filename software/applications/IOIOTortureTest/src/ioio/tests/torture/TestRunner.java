package ioio.tests.torture;

import java.util.concurrent.TimeoutException;

import ioio.lib.api.exception.ConnectionLostException;

interface TestRunner {
	public void run() throws ConnectionLostException, InterruptedException, TimeoutException;
	public String testClassName();
}
