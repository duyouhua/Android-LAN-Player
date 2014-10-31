package com.cvte.lanplayer.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.cvte.lanplayer.GlobalData;

public class ScanLanDeviceUtil {

	private static String TAG = "ScanLanDeviceUtil";
	private static Service mService;
	private static ScanLanDeviceUtil instance = null;

	// ɨ��ͽ��շ���ָ����߳�
	private static TcpReceiveThread mTCPThread;

	private DatagramSocket udpSocket;
	private Socket socket = null;
	private ServerSocket ss = null;

	private boolean isTCPThreadStart = false;
	private boolean isBroadCastUdpThreadStart = false;

	private static final int MAX_DATA_PACKET_LENGTH = 40;
	private byte[] buffer = new byte[MAX_DATA_PACKET_LENGTH];

	/**
	 * ˽��Ĭ�Ϲ�����
	 */
	private ScanLanDeviceUtil() {

		mTCPThread = new TcpReceiveThread();
	}

	/**
	 * ��̬��������
	 */
	public static synchronized ScanLanDeviceUtil getInstance(Service service) {

		mService = service;
		if (instance == null) {
			instance = new ScanLanDeviceUtil();
		}

		return instance;
	}

	/**
	 * ֹͣɨ�� ��տ������̺߳�socket
	 * 
	 * @throws IOException
	 */
	public void StopScan() throws IOException {
		if (mTCPThread != null) {
			Log.d(TAG, "interrupt mTCPThread");

			mTCPThread.interrupt();
		}

		if (udpSocket != null) {
			udpSocket.close();
		}

		if (socket != null) {
			socket.close();
		}

		if (ss != null) {
			ss.close();
		}

	}

	/**
	 * ��ʼɨ��
	 * 
	 * ��ʼɨ��֮�󣬽��շ�Ҫ����һ��GlobalData.GET_SCAN_DATA_ACTION �Ĺ㲥���������ܽ�����������IP��ַ
	 */
	public void StartScan() {

		Log.d(TAG, "StartScan");

		// ÿ����һ�Σ��Ϳ���һ��UDP�߳̽���ɨ��
		new BroadCastUdpThread(getLocalIPAddress().toString()).start();

		// ����TCP�ش���Ϣ���̣߳�һֱ���������ᱻ��
		if (mTCPThread.getState() == Thread.State.NEW) {

			mTCPThread.start();
		}

	}

	private String getLocalIPAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, ex.toString());
		}
		return null;
	}

	/**
	 * �ҵ���Ӧ��IP��ַ������й㲥
	 * 
	 * @param str
	 */
	private void SendMessage(String str) {
		Intent intent = new Intent();
		intent.putExtra("str", str);
		intent.setAction(GlobalData.GET_SCAN_DATA_ACTION);// action���������ͬ

		mService.sendBroadcast(intent);
	}

	/**
	 * 
	 * UDPɨ�����
	 * 
	 * @author JHYin
	 * 
	 */
	public class BroadCastUdpThread extends Thread {
		private String dataString;
		private int BroadCastUdpCount = 10; // ������¼����Ĺ㲥����

		public BroadCastUdpThread(String dataString) {
			this.dataString = dataString;
		}

		public void run() {

			/*
			 * �رս��ն˿�ɨ��
			 * �������synchronized()ͬ�������֮ǰ�������޷������
			 */
			StopLANRecv();

			/*
			 * ʹ��ͬ���������߳�ͬ��
			 * 
			 * ����ʹ��wait()��nodify() ��Ϊ�ڽ���synchronized()�Ĵ�����ʱ�򣬾ʹ�����������Ȼ�����û������
			 * ��ô��Ҫ���еȴ���������wait(); ��synchronized()�Ĵ����ִ����ϣ��ͻ��Զ��ͷ�����������nodify()֪ͨ
			 */
			synchronized (GlobalData.UDP_SOCKET_LOCK) {
				DatagramPacket dataPacket = null;
				try {
					udpSocket = new DatagramSocket(GlobalData.UDP_PORT);

					dataPacket = new DatagramPacket(buffer,
							MAX_DATA_PACKET_LENGTH);
					byte[] data = dataString.getBytes();
					dataPacket.setData(data);
					dataPacket.setLength(data.length);
					dataPacket.setPort(GlobalData.UDP_PORT);

					InetAddress broadcastAddr;

					broadcastAddr = InetAddress.getByName("255.255.255.255");
					dataPacket.setAddress(broadcastAddr);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}

				try {
					// �����ι㲥 -- ���߲��÷�����ι㲥
					for (int i = 0; i < BroadCastUdpCount; i++) {
						udpSocket.send(dataPacket);

						Log.d(TAG, "send udpSocket in BroadCastUdpThread");

						// ����100ms
						sleep(100);
					}
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				} finally {
					if (udpSocket != null) {
						udpSocket.close();
					}

				}

				// �����������ն˿�
				StartLANRecv();
			}
		}
	}

	/**
	 * ����ɨ��
	 */
	private void StartLANRecv() {
		Intent intent = new Intent();

		Bundle data = new Bundle();
		data.putInt(GlobalData.GET_BUNDLE_COMMANT, GlobalData.STARE_LAN_RECV);

		intent.putExtras(data);
		intent.setAction(GlobalData.CTRL_RECV_ACTION);// action���������ͬ

		mService.sendBroadcast(intent);
	}

	/**
	 * ֹͣɨ��
	 */
	private void StopLANRecv() {
		Intent intent = new Intent();

		Bundle data = new Bundle();
		data.putInt(GlobalData.GET_BUNDLE_COMMANT, GlobalData.STOP_LAN_RECV);

		intent.putExtras(data);
		intent.setAction(GlobalData.CTRL_RECV_ACTION);// action���������ͬ

		mService.sendBroadcast(intent);
	}

	private class TcpReceiveThread extends Thread {
		public void run() {

			while (true) {

				BufferedReader in = null;
				try {
					ss = new ServerSocket(GlobalData.SOCKET_PORT);

					socket = ss.accept();

					if (socket != null) {

						in = new BufferedReader(new InputStreamReader(
								socket.getInputStream()));

						StringBuilder sb = new StringBuilder();
						sb.append(socket.getInetAddress().getHostAddress());
						String line = null;
						while ((line = in.readLine()) != null) {
							sb.append(line);
						}
						Log.i("TcpReceive", "connect :" + sb.toString());

						final String ipString = sb.toString().trim();// "192.168.0.104:8731";

						SendMessage(ipString);
					}

				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (in != null)
							in.close();
						if (socket != null)
							socket.close();
						if (ss != null)
							ss.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
			// isTCPThreadStart = false;

		}
	}

}