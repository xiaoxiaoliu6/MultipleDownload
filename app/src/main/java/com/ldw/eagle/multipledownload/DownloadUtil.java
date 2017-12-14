package com.ldw.eagle.multipledownload;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 eagle
 *
 */
public class DownloadUtil {

	private static DownloadUtil instance = null;
	private Context context = null;
	private List<String> downloadList = null;
	private Map<String,DownloadHttpTool> downloadMap = null;
	private int currentUrlIndex = -1;
	private final int MAX_COUNT = 2;
	private int currentCount = 0;
	private final String FLAG_FREE = "free";
	private OnDownloadListener onDownloadListener = null;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String url = msg.obj.toString();
			if (msg.what == 0) {
				if (onDownloadListener != null) {
					onDownloadListener
							.downloadProgress(url, msg.arg2, msg.arg1);
				}
			} else if (msg.what == 1) {
				if (onDownloadListener != null) {
					onDownloadListener.downloadStart(url, msg.arg1);
				}
			} else if (msg.what == 2) {
				onDownloadListener.downloadEnd(url);
			}
		}

	};

	private DownloadUtil(Context context) {
		this.context = context;
		downloadList = new ArrayList<String>();
		downloadMap = new HashMap<String, DownloadHttpTool>();
	}

	private static synchronized void syncInit(Context context) {
		if (instance == null) {
			instance = new DownloadUtil(context);
		}
	}

	public static DownloadUtil getInstance(Context context) {
		if (instance == null) {
			syncInit(context);
		}
		return instance;
	}

	public void prepare(String urlString) {
		downloadList.add(urlString);
		if (currentCount < MAX_COUNT) {
			start();
		} else {
			LogUtils.d("等待下载____" + urlString);
		}
	}


	private synchronized void start() {
		if (++currentUrlIndex >= downloadList.size()) {
			currentUrlIndex--;
			return;
		}
		currentCount++;
		String urlString = downloadList.get(currentUrlIndex);
		LogUtils.d("开始下载____" + urlString);
		DownloadHttpTool downloadHttpTool = null;
		if (downloadMap.size() < MAX_COUNT) { // 保证downloadMap.size() <= 2
			downloadHttpTool = new DownloadHttpTool(context, mHandler,
					downloadComplated);
			if (downloadMap.containsKey(urlString)) {
				downloadMap.remove(urlString);
			}
			downloadMap.put(urlString, downloadHttpTool);
		} else {
			downloadHttpTool = downloadMap.get(FLAG_FREE);
			downloadMap.remove(FLAG_FREE);
			downloadMap.put(urlString, downloadHttpTool);
		}
		downloadHttpTool.start(urlString);
	}

	public void pause(String urlString) {
		paused(urlString, new Paused() {

			@Override
			public void onPaused(DownloadHttpTool downloadHttpTool) {
				downloadHttpTool.pause();
			}
		});
	}


	public void pauseAll() {

		String[] keys = new String[downloadMap.size()];
		downloadMap.keySet().toArray(keys);
		for (int i = keys.length - 1; i >= 0; i--) {
			pause(keys[i]);
		}
		instance = null;
	}


	public void resume(String urlString) {
		prepare(urlString);
	}


	public void resumeAll() {
		for (Entry<String, DownloadHttpTool> entity : downloadMap.entrySet()) {
			prepare(entity.getKey());
		}
	}


	public void delete(String urlString) {
		boolean bool = paused(urlString, new Paused() {

			@Override
			public void onPaused(DownloadHttpTool downloadHttpTool) {
				downloadHttpTool.pause();
				downloadHttpTool.delete();
			}
		});
		if (!bool) {
			File file = new File(DownloadHttpTool.filePath + "/"
					+ urlString.split("/")[urlString.split("/").length - 1]
					+ DownloadHttpTool.FILE_TMP_SUFFIX);
			LogUtils.d(String.valueOf(file.delete()));
		}
	}

	interface Paused {
		void onPaused(DownloadHttpTool downloadHttpTool);
	}

	private boolean paused(String urlString, Paused paused) {
		if (downloadMap.containsKey(urlString)) {
			currentCount--;
			DownloadHttpTool downloadHttpTool = downloadMap.get(urlString);
			paused.onPaused(downloadHttpTool);
			if (!downloadMap.containsKey(FLAG_FREE)) { // 保证key == FLAG_FREE的数量
														// = 1
				downloadMap.put(FLAG_FREE, downloadHttpTool);
			}
			downloadMap.remove(urlString);
			start();
			return true;
		}
		return false;
	}

	DownloadHttpTool.DownloadComplated downloadComplated = new DownloadHttpTool.DownloadComplated() {

		@Override
		public void onComplated(String urlString) {
			LogUtils.d("下载完成____" + urlString);
			Message msg = new Message();
			msg.what = 2;
			msg.obj = urlString;
			mHandler.sendMessage(msg);
			pause(urlString);

			if (downloadMap.size() == 1 && downloadMap.containsKey(FLAG_FREE)) {
				LogUtils.d("全部下载结束");
			}
		}
	};

	public void setOnDownloadListener(OnDownloadListener onDownloadListener) {
		this.onDownloadListener = onDownloadListener;
	}

	public interface OnDownloadListener {

		public void downloadStart(String url, int fileSize);


		public void downloadProgress(String url, int downloadedSize, int length);


		public void downloadEnd(String url);

	}

}