package net.gliby.minecraft.udp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.apache.logging.log4j.Logger;

import com.esotericsoftware.minlog.Log;

public class AnotherLogger extends Log.Logger {

	Logger logger;

	public AnotherLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void log(int level, String category, String message, Throwable ex) {
		StringBuilder builder = new StringBuilder(256);
		switch (level) {
		case Log.LEVEL_ERROR:
			logger.error(message);
			break;
		case Log.LEVEL_WARN:
			logger.warn(message);
			break;
		case Log.LEVEL_INFO:
			logger.info(message);
			break;
		case Log.LEVEL_DEBUG:
			logger.debug(message);
			break;
		case Log.LEVEL_TRACE:
			logger.trace(message);
			break;
		}
	}

}
