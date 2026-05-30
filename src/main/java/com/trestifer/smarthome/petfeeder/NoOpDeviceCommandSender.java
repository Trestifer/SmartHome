package com.trestifer.smarthome.petfeeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpDeviceCommandSender implements DeviceCommandSender {

	private static final Logger log = LoggerFactory.getLogger(NoOpDeviceCommandSender.class);

	@Override
	public void sendCommand(String deviceCode, long commandId, String commandType, String portionSize) {
		log.info("Device command queued for later transport: deviceCode={}, commandId={}, commandType={}, portionSize={}",
				deviceCode, commandId, commandType, portionSize);
	}
}
