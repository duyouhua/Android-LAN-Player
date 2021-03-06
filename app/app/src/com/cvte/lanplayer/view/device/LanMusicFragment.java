package com.cvte.lanplayer.view.device;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.cvte.lanplayer.GlobalData;
import com.cvte.lanplayer.R;
import com.cvte.lanplayer.adapter.MyListAdapter;
import com.cvte.lanplayer.entity.SocketTranEntity;
import com.cvte.lanplayer.utils.SendSocketMessageUtil;

public class LanMusicFragment extends Fragment {

	private final String TAG = "LanMusicFragment";

	private String targetIp = null;

	private Button btn_request_musiclist;
	private ListView lv_music_list;

	private List<String> mMusicList = new ArrayList<String>();
	private MyListAdapter mMusicListAdapter;

	private RecvScoketMsgReceiver mRecvScoketMsgReceiver;

	private Activity mActivity;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.mActivity = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_lan_music, container,
				false);

		btn_request_musiclist = (Button) view
				.findViewById(R.id.btn_request_musiclist);
		lv_music_list = (ListView) view.findViewById(R.id.lv_music_list);

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onViewCreated(view, savedInstanceState);

		// 通过Bundle获取Activity里面的数据
		Bundle bundle = getArguments();
		targetIp = bundle.getString(GlobalData.GetBundle.GET_IP);

		// 注册接收器
		mRecvScoketMsgReceiver = new RecvScoketMsgReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(GlobalData.SocketTranCommand.RECV_SOCKET_FROM_SERVICE_ACTION);
		mActivity.registerReceiver(mRecvScoketMsgReceiver, filter);

		// 设置监听
		SetListener();
		// 请求获得目标IP的音乐列表
		GetMusicList();
	}

	/**
	 * 设置监听
	 */
	private void SetListener() {
		// TODO Auto-generated method stub
		btn_request_musiclist.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Log.d(TAG, "on click button");

				GetMusicList();
			}
		});

		// 为ListView设置Adapter
		mMusicListAdapter = new MyListAdapter(mMusicList, mActivity);
		lv_music_list.setAdapter(mMusicListAdapter);

		// 设置ListView监听
		lv_music_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// 打开确定请求获取对话框
				ShowRequsetDialog(position);
			}
		});
	}

	/**
	 * 弹出是否请求拉取对话框
	 * 
	 */
	private void ShowRequsetDialog(final int musicID) {

		String musicName = mMusicList.get(musicID);

		AlertDialog.Builder builder = new Builder(mActivity);

		// 设置标题
		builder.setTitle("获取音乐");
		// 设置描述信息
		builder.setMessage("是否拉取：" + musicName);

		// 确认
		builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				Log.d(TAG, "on click:请求获取音乐：" + String.valueOf(musicID));

				// 请求获取目标IP的相应的音乐文件
				// 实例化传输对象
				SocketTranEntity msg = new SocketTranEntity();
				msg.setmCommant(GlobalData.SocketTranCommand.COMMAND_REQUSET_MUSIC_FILE);
				// 以目标IP的音乐列表的编号作为标记
				msg.setmMessage(String.valueOf(musicID));
				msg.setmSendIP(getIpAddress());

				SendSocketMessageUtil.getInstance(mActivity).SendMessage(msg,
						targetIp);

			}
		});

		// 取消
		builder.setPositiveButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
			}
		});

		builder.show();
	}

	/**
	 * 获取本机的IP地址
	 * 
	 * 以后重构把这里去掉
	 * 
	 */
	private String getIpAddress() {
		WifiManager wifiManager = (WifiManager) mActivity
				.getSystemService(mActivity.WIFI_SERVICE);

		WifiInfo wifiInfo = wifiManager.getConnectionInfo();

		int ipAddress = wifiInfo.getIpAddress();
		// Log.d("TAG","IP:"+ String.valueOf(ipAddress));

		return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "."
				+ (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));

	}

	/**
	 * 请求目标IP，获取其音乐列表
	 */
	private void GetMusicList() {
		// 发送获取音乐列表的请求
		// 实例化传输对象
		SocketTranEntity msg = new SocketTranEntity();
		msg.setmCommant(GlobalData.SocketTranCommand.COMMAND_REQUSET_MUSIC_LIST);

		SendSocketMessageUtil.getInstance(mActivity).SendMessage(msg, targetIp);
	}

	// 获取扫描出来的IP地址的接收器
	private class RecvScoketMsgReceiver extends BroadcastReceiver {

		// 自定义一个广播接收器
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "RECV:COMMAND");

			Bundle bundle = intent.getExtras();

			// 获取指令
			int commant = bundle
					.getInt(GlobalData.SocketTranCommand.GET_BUNDLE_COMMANT);

			// 假如是发送音乐列表
			if (commant == GlobalData.SocketTranCommand.COMMAND_SEND_MUSIC_LIST) {
				Log.d(TAG, "RECV:COMMAND_SEND_MUSIC_LIST");

				SocketTranEntity data = (SocketTranEntity) bundle
						.getSerializable(GlobalData.SocketTranCommand.GET_BUNDLE_COMMON_DATA);

				// 把收到的数据显示出来
				mMusicList.clear(); // 清空列表
				for (int i = 0; i < data.getmMusicList().size(); i++) {
					mMusicList.add(data.getmMusicList().get(i).getFileName());
				}

				// 更新数据
				mMusicListAdapter.notifyDataSetChanged();

			}
		}
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();

		// 结束的时候取消注册广播接收器，否则报错
		mActivity.unregisterReceiver(mRecvScoketMsgReceiver);
	}
}
