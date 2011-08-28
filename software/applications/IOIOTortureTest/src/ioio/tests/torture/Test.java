package ioio.tests.torture;

import java.util.concurrent.TimeoutException;

import ioio.lib.api.exception.ConnectionLostException;

interface Test<E> {
	E run() throws ConnectionLostException, InterruptedException, TimeoutException;
}
