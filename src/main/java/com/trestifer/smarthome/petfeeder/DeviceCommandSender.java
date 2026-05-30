package com.trestifer.smarthome.petfeeder;

public interface DeviceCommandSender {

	void sendCommand(String deviceCode, long commandId, String commandType, String portionSize);
}
