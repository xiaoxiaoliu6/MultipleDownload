package com.ldw.eagle.multipledownload;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**

 * eagle
 */
public class DownloadHttpTool {

	private final int THREAD_COUNT = 3;
	private String urlstr = "";
	private Context mContext = null;
	private List<DownloadInfo> downloadInfos = null;

	public static String filePath = "";
	private String fileName = "";
	private String fileNameTmp = "";

	public static final String FILE_TMP_SUFFIX = ".tmp";
	private int fileSize = 0;
	private DownlaodSqlTool sqlTool = null;
	private DownloadComplated downloadComplated = null;
	private int totalCompelete = 0;
	private List<DownloadThread> threads = null;
	private Handler handler = null;

	private enum Download_State {
		Downloading, Pause, Ready, Completed, Exception;
	}

	private Download_State state = Download_State.Ready; // 下载状态


	public DownloadHttpTool(Context context, Handler handler,
			DownloadComplated downloadComplated) {
		super();
		this.mContext = context;
		this.handler = handler;
		this.downloadComplated = downloadComplated;
		sqlTool = DownlaodSqlTool.getInstance(mContext);
		if ("".equals(filePath)) {
			filePath = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/eagleDownload";
		}
		threads = new ArrayList<DownloadThread>();
	}


	public void start(String urlstr) {
		this.urlstr = urlstr;
		String[] ss = urlstr.split("/");
		fileName = ss[ss.length - 1];
		fileNameTmp = fileName + FILE_TMP_SUFFIX;

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... arg0) {

				ready();
				Message msg = new Message();
				msg.what = 1;
				msg.arg1 = fileSize;
				msg.obj = DownloadHttpTool.this.urlstr;
				handler.sendMessage(msg);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				startDownload();
			}
		}.execute();
	}


	private void ready() {
		if (new File(filePath + "/" + fileName).exists()) {
			downloadComplated.onComplated(urlstr);
			return;
		}
		totalCompelete = 0;
		downloadInfos = sqlTool.getInfos(urlstr);
		if (downloadInfos.size() == 0) {
			initFirst();
		} else {
			File file = new File(filePath + "/" + fileNameTmp);
			if (!file.exists()) {
				sqlTool.delete(urlstr);
				initFirst();
			} else {
				fileSize = downloadInfos.get(downloadInfos.size() - 1)
						.getEndPos();
				for (DownloadInfo info : downloadInfos) {
					totalCompelete += info.getCompleteSize();
				}
			}
		}
	}


	private void startDownload() {
		if (downloadInfos != null) {
			if (state == Download_State.Downloading) {
				return;
			}
			state = Download_State.Downloading;
			for (DownloadInfo info : downloadInfos) {
				DownloadThread thread = new DownloadThread(info.getThreadId(),
						info.getStartPos(), info.getEndPos(),
						info.getCompleteSize(), info.getUrl());
				thread.start();
				threads.add(thread);
			}
		}
	}


	public void pause() {
		state = Download_State.Pause;
	}


	public void delete() {
		compeleted();
		File file = new File(filePath + "/" + fileNameTmp);
		file.delete();
	}


	private void compeleted() {
		state = Download_State.Completed;
		sqlTool.delete(urlstr);
		downloadComplated.onComplated(urlstr);
	}


	public int getFileSize() {
		return fileSize;
	}


	public int getTotalCompeleteSize() {
		return totalCompelete;
	}


	private void initFirst() {
		URL url = null;
		RandomAccessFile accessFile = null;
		HttpURLConnection connection = null;
		try {
			url = new URL(urlstr);
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(5000);
			connection.setRequestMethod("GET");
			fileSize = connection.getContentLength();
			if (fileSize < 0) {
				return;
			}

			File fileParent = new File(filePath);
			if (!fileParent.exists()) {
				fileParent.mkdir();
			}
			File file = new File(fileParent, fileNameTmp);
			if (!file.exists()) {
				file.createNewFile();
			}

			accessFile = new RandomAccessFile(file, "rwd");
			accessFile.setLength(fileSize);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (accessFile != null) {
				try {
					accessFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
		// 计算每个线程需要下载的大小
		int range = fileSize / THREAD_COUNT;
		// 保存每个线程的下载信息
		downloadInfos = new ArrayList<DownloadInfo>();
		for (int i = 0; i < THREAD_COUNT - 1; i++) {
			DownloadInfo info = new DownloadInfo(i, i * range, (i + 1) * range
					- 1, 0, urlstr);
			downloadInfos.add(info);
		}
		// 最后一个线程和前面的处理有点不一样
		DownloadInfo info = new DownloadInfo(THREAD_COUNT - 1,
				(THREAD_COUNT - 1) * range, fileSize - 1, 0, urlstr);
		downloadInfos.add(info);
		// 插入到数据库
		sqlTool.insertInfos(downloadInfos);
	}

	interface DownloadComplated {

		/**
		 * 下载完成回调
		 * 
		 * @param urlString
		 */
		void onComplated(String urlString);

	}

	/** 自定义下载线程 */
	private class DownloadThread extends Thread {

		private int threadId = 0; // 线程Id
		private int startPos = 0; // 在文件中的开始的位置
		private int endPos = 0; // 在文件中的结束的位置
		private int compeleteSize = 0; // 已完成下载的大小
		private String urlstr = ""; // 下载地址

		/**
		 * 
		 * @param threadId
		 *            线程Id
		 * @param startPos
		 *            在文件中的开始的位置
		 * @param endPos
		 *            在文件中的结束的位置
		 * @param compeleteSize
		 *            已完成下载的大小
		 * @param urlstr
		 *            下载地址
		 */
		public DownloadThread(int threadId, int startPos, int endPos,
				int compeleteSize, String urlstr) {
			this.threadId = threadId;
			this.startPos = startPos;
			this.endPos = endPos;
			this.urlstr = urlstr;
			this.compeleteSize = compeleteSize;
		}

		@Override
		public void run() {
			HttpURLConnection connection = null;
			RandomAccessFile randomAccessFile = null;
			InputStream is = null;
			try {
				randomAccessFile = new RandomAccessFile(filePath + "/"
						+ fileNameTmp, "rwd");
				randomAccessFile.seek(startPos + compeleteSize);
				URL url = new URL(urlstr);
				connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(5000);
				connection.setRequestMethod("GET");
				// 设置请求的数据的范围
				connection.setRequestProperty("Range", "bytes="
						+ (startPos + compeleteSize) + "-" + endPos);
				is = connection.getInputStream();
				byte[] buffer = new byte[6 * 1024]; // 6K的缓存
				int length = -1;
				while ((length = is.read(buffer)) != -1) {
					randomAccessFile.write(buffer, 0, length); // 写缓存数据到文件
					compeleteSize += length;
					synchronized (this) { // 加锁保证已下载的正确性
						totalCompelete += length;
						Message msg = new Message();
						msg.what = 0;
						msg.arg1 = length;
						msg.arg2 = totalCompelete;
						msg.obj = urlstr;
						handler.sendMessage(msg);
					}
					// 非正在下载状态时跳出循环
					if (state != Download_State.Downloading) {
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(new StringBuilder().append("异常退出____").append(urlstr).append("---->").append(e.getMessage()).toString());
				state = Download_State.Exception;
			} finally {
				// 不管发生了什么事，都要保存下载信息到数据库
				sqlTool.updataInfos(threadId, compeleteSize, urlstr);
				if (threads.size() == 1) { // 当前线程是此url对应下载任务唯一一个正在执行的线程
					try {
						if (is != null) {
							is.close();
						}
						if (randomAccessFile != null) {
							randomAccessFile.close();
						}
						if (connection != null) {
							connection.disconnect();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (state == Download_State.Downloading) { // 此时此线程的下载任务正常完成（没有被人为或异常中断）
						File file = new File(filePath + "/" + fileNameTmp);
						file.renameTo(new File(filePath + "/" + fileName));
					}
					if (state != Download_State.Pause) {
						compeleted();
					}
				}
				threads.remove(this);
			}
		}
	}
}