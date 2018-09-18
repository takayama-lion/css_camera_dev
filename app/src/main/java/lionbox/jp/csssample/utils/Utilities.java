package lionbox.jp.csssample.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Utilities {

    /**
     * get ipv 4 address
     * @return
     */
    public static String getIpV4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                Enumeration<InetAddress> addresses = network.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    String address = addresses.nextElement().getHostAddress();

                    //127.0.0.1と0.0.0.0以外のアドレスが見つかったらそれを返す
                    if (!"127.0.0.1".equals(address) && !"0.0.0.0".equals(address)
                            && address.matches("((\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])([.](?!$)|$)){4}")) {
                        return address;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
