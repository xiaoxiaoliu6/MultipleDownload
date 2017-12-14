package com.ldw.eagle.multipledownload;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * eagle
 */
public class DownlaodSqlTool {
	
	private static DownlaodSqlTool instance = null;
	private DownLoadHelper dbHelper = null;

	private DownlaodSqlTool(Context context) {
		dbHelper = new DownLoadHelper(context);
	}

	private static synchronized void syncInit(Context context) {
		if (instance == null) {
			instance = new DownlaodSqlTool(context);
		}
	}

	public static DownlaodSqlTool getInstance(Context context) {
		if (instance == null) {
			syncInit(context);
		}
		return instance;
	}
	

	public void insertInfos(List<DownloadInfo> infos) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		for (DownloadInfo info : infos) {
			String sql = "insert into download_info(thread_id,start_pos, end_pos,complete_size,url) values (?,?,?,?,?)";
			Object[] bindArgs = { info.getThreadId(), info.getStartPos(),
					info.getEndPos(), info.getCompleteSize(), info.getUrl()};
			database.execSQL(sql, bindArgs);
		}
	}


	public List<DownloadInfo> getInfos(String urlstr) {
		List<DownloadInfo> list = new ArrayList<DownloadInfo>();
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		String sql = "select thread_id, start_pos, end_pos,complete_size,url from download_info where url=?";
		Cursor cursor = database.rawQuery(sql, new String[] { urlstr });
		while (cursor.moveToNext()) {
			DownloadInfo info = new DownloadInfo(cursor.getInt(0),
					cursor.getInt(1), cursor.getInt(2), cursor.getInt(3),
					cursor.getString(4));
			list.add(info);
		}
		cursor.close();
		return list;
	}


	public void updataInfos(int threadId, int compeleteSize, String urlstr) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		String sql = "update download_info set complete_size=? where thread_id=? and url=?";
		Object[] bindArgs = { compeleteSize, threadId, urlstr };
		database.execSQL(sql, bindArgs);
	}


	public void closeDb() {
		dbHelper.close();
	}


	public void delete(String url) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		database.delete("download_info", "url=?", new String[] { url });
	}
}