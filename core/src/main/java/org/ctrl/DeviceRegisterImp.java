/*
 * $Id: DeviceRegisterImp.java,v 1.3 2005/06/25 02:54:51 remus Exp $
 *
 *  Copyright [2005] [Remus Pereni http://remus.pereni.org]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.ctrl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://remus.pereni.org>Remus Pereni</a>
 *
 */
public class DeviceRegisterImp implements IDeviceRegister {

    protected ArrayList<IDevice> devices = new ArrayList<IDevice>();
    protected ArrayList<Integer> devicesId = new ArrayList<Integer>();
    protected ArrayList<String> devicesMnemonic = new ArrayList<String>();
    protected static IDeviceRegister instance = new DeviceRegisterImp();
    public static final IDevice UNKNOWN_DEVICE = new UnknownDevice();

    /**
     * 
     */
    protected DeviceRegisterImp() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.DeviceRegister#addDevice(org.pereni.ctrl.Device)
     */
    public void addDevice(IDevice device) {
        if (device != null) {
            devices.add(device);
            devicesId.add(device.getId());
            if (device.getMnemonic() != null) {
                devicesMnemonic.add(device.getMnemonic());
            } else {
                devicesMnemonic.add("");
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.DeviceRegister#lookupById(int)
     */
    public IDevice lookupById(int id) {
        int devPosition = devicesId.indexOf(id);

        if (devPosition != -1) {
            return devices.get(devPosition);
        }

        return UNKNOWN_DEVICE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.DeviceRegister#lookupByMnemonic(java.lang.String)
     */
    public IDevice lookupByMnemonic(String mnemonic) {
        int devPosition = devicesMnemonic.indexOf(mnemonic);

        if (devPosition != -1) {
            return devices.get(devPosition);
        }

        return UNKNOWN_DEVICE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.DeviceRegister#getDevices()
     */
    public List<IDevice> getDevices() {
        return devices;
    }

    public static IDeviceRegister getInstance() {
        return instance;
    }
}
