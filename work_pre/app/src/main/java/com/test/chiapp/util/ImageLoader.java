package com.test.chiapp.util;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 
 * @author weiyuchi-2017.05.06
 * @version 加载图片----单例模式  （不完善）
 */
@SuppressLint("HandlerLeak")
public class ImageLoader {
	final private String TAG = "ImageLoader(图片加载器)：";
	private static ImageLoader mInstance;
	/*
	 * 图片存储地址
	 */
	final private String PATH = Environment.getExternalStorageDirectory().getPath()+"/image/";
	/**
	 * 图片缓存的核心对象
	 */
	private LruCache<String, Bitmap> mLruCache;
	/**
	 * 线程池0
	 */
	private ExecutorService mThreadPool;
	/**
	 * 默认线程数
	 */
	private static final int DEFAULT_THREAD_COUNT = 3;
	/**
	 * 队列调度方式
	 */
	private Type myType = Type.LIFO;
	/**
	 * 任务队列
	 */
	private LinkedList<Runnable> mTaskQueue;
	/**
	 * 后台轮询线程
	 */
	private Thread mPoolThread;
	/**
	 * 给mPoolThread发送消息
	 */
	private Handler mPoolThreadHander;
	/**
	 * UI线程中的Hander
	 */
	private Handler mUIHander;
	/**
	 * 信号量：
	 * 确保异步线程的执行，确保mPoolThreadHander!=null
	 */
	private Semaphore mSemaphorePoolThreadHander = new Semaphore(0);
	/**
	 * 信号量：
	 */
	private Semaphore mSemaphoreThreadPool;
	/**
	 * 从线程池中抽取线程方式
	 * @FIFO 从最开始取
	 * @LIFO 从最后的取
	 */
	public enum Type {
		FIFO, LIFO
	}
	private ImageLoader() {
		init();
	}
	/**
	 * 获取到单例对象 （注意：构造方法设为private的目的是不允许new 出一个新对象）
	 * @return
	 */
	public static ImageLoader getInstance() {
		if (mInstance == null) {
			synchronized (ImageLoader.class) {
				if (mInstance == null) {
					mInstance = new ImageLoader();
				}
			}
		}
		return mInstance;
	}
	/**
	 * 初始化
	 */
	private void init() {
		// 后台轮询线程
		mPoolThread = new Thread() {
			@Override
			public void run() {
				Looper.prepare();
				mPoolThreadHander = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						// 线程池去取出一个任务进行执行
						mThreadPool.execute(getTask());

						try {
							mSemaphoreThreadPool.acquire();
						} catch (InterruptedException e) {
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
					}
				};
				// 释放一个信号量
				mSemaphorePoolThreadHander.release();
				Looper.loop();// 轮询
			};
		};

		mPoolThread.start();

		// 获取我们应用最大可用内存
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheMemory = maxMemory / 8;

		mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getRowBytes() * value.getHeight();// 每个Bitmap所占据的内存
			}
		};
		// 创建线程池
		mThreadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
		mTaskQueue = new LinkedList<Runnable>();
		mSemaphoreThreadPool = new Semaphore(DEFAULT_THREAD_COUNT);
	}
	/**
	 * 根据path为imageView设置图片
	 * 
	 * @param path
	 * @param imageView
	 */
	public void LoadImage(final String path, final ImageView imageView) {//加个 boolean 判断是否是网路图片
		if (path.equals("")||imageView==null)return;
		imageView.setTag(path);
		if (mUIHander == null) {
			mUIHander = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					// 获得图片，为imageView回调设置图片
					ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
					Bitmap bm = holder.bitmap;
					ImageView imageView = holder.imageView;
					String path = holder.path;
					
					// 将path与getTag存储路径进行比较
					if (imageView.getTag().toString().equals(path)) {
						imageView.setImageBitmap(bm);
					}
				};
			};
		}

		Bitmap bm = getBitmapFromLruCache(path);
		if (bm != null) {
			refreashBitmap(path, imageView, bm);
		} else {
			addTask(new Runnable() {

				@Override
				public void run() {//可以  在这处理网络图片
					
					// 加载图片
					// 图片的压缩
					// 1.获得图片需要显示的大小 
//					ImageSize imageSize = getImageViewSize(imageView);
					// 2.压缩图片
//					Bitmap bm = decodeSamplenBitmapFromPath(path, imageSize.width, imageSize.height);
				
					downloadPhoto(imageView, path);	
					mSemaphoreThreadPool.release();
				}
			});
		}
	}

	private void refreashBitmap(final String path, final ImageView imageView, Bitmap bm) {
		Message message = Message.obtain();
		ImgBeanHolder holder = new ImgBeanHolder();
		holder.bitmap = bm;
		holder.imageView = imageView;
		holder.path = path;
		message.obj = holder;
		mUIHander.sendMessage(message);
	}

	/**
	 * 将图片加入缓存
	 * 
	 * @param path
	 * @param bm
	 */
	protected void addBitmapToLruCache(String path, Bitmap bm) {
		if (getBitmapFromLruCache(path) == null) {
			if (bm != null) {
				mLruCache.put(path, bm);
			}
		}
	}

	/**
	 * 
	 * 根据图片需要显示的宣和高对图片进行压缩
	 * 
	 * @param path
	 * @param width
	 * @param height
	 * @return
	 */
	protected Bitmap decodeSamplenBitmapFromPath(String path, int width, int height) {
		// 获取图片宽和高，并不把图片加载到内存当中
		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		options.inSampleSize = caculatenInSampleSize(options, width, height);
		// 使用获取到的inSampleSize再次解析图片
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeFile(path, options);

		return bitmap;
	}

	/**
	 * 根据需求的宽和高以及图片的实际的宽和高计算SampleSize
	 * 
	 * @param options
	 * @param width
	 * @param height
	 * @return
	 */
	private int caculatenInSampleSize(Options options, int reqwidth, int reqheight) {
		int width = options.outWidth;
		int height = options.outHeight;

		int inSampleSize = 1;
		if (width > reqwidth || height > reqheight) {
			int widthRadio = Math.round(width * 1.0f / reqwidth);
			int heightRadio = Math.round(height * 1.0f / reqheight);
			inSampleSize = Math.max(widthRadio, heightRadio);
		}
		return inSampleSize;
	}
	/**
	 * 向线程池中添加线程 
	 */
	private synchronized void addTask(Runnable runnable) {
		mTaskQueue.add(runnable);
		try {
			if (mPoolThreadHander == null) {
				mSemaphorePoolThreadHander.acquire();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mPoolThreadHander.sendEmptyMessage(0x110);
	}

	/**
	 * 根据path在缓存中获取Bitmap
	 */
	private Bitmap getBitmapFromLruCache(String path) {
		return mLruCache.get(path);
	}

	private class ImageSize {
		int width;
		int height;
	}

	private class ImgBeanHolder {
		Bitmap bitmap;
		ImageView imageView;
		String path;
	}

	/**
	 * 从任务队列取出一个方法
	 * 
	 * @return
	 */
	private Runnable getTask() {
		if (myType == Type.FIFO) {
			return mTaskQueue.removeFirst();
		} else if (myType == Type.LIFO) {
			return mTaskQueue.removeLast();
		}
		return null;
	}

	/**
	 * 根据ImageView获取适当的宽和高
	 * 
	 * @param imageView
	 * @return
	 */
	@SuppressLint("NewApi")
	protected ImageSize getImageViewSize(ImageView imageView) {
		ImageSize imageSize = new ImageSize();

		DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();

		LayoutParams lp = imageView.getLayoutParams();
		int width = imageView.getWidth();// 获取imageView的实际宽度
		// int width =
		// (lp.width==LayoutParams.WRAP_CONTENT?0:imageView.getWidth());
		if (width <= 0) {
			width = lp.width;// 获取imageView 在layout中声明的高度
		}
		if (width <= 0) {
		//	width = imageView.getMaxWidth();// 检查最大值
			width = getImageViewFieldValue(imageView, "mXaxWidth");// 检查最大值
		}
		if (width <= 0) {
			width = displayMetrics.widthPixels;
		}
		int height = imageView.getHeight();// 获取imageView的实际高度
		if (height <= 0) {
			height = lp.height;// 获取imageView 在layout中声明的高度
		}
		if (height <= 0) {
			//height = imageView.getMaxHeight();// 检查最大值
			height =getImageViewFieldValue(imageView, "mXaxheight");
		}
		if (height <= 0) {
			height = displayMetrics.heightPixels;
		}
		imageSize.width = width;
		imageSize.height = height;
		return imageSize;
	}

	/**
	 * 通过反射获取imageView的某个属性值
	 * 
	 * @param object
	 * @param fieldName
	 * @return
	 */
	private int getImageViewFieldValue(Object object, String fieldName) {
		int value = 0;
		try {
			Field field = ImageView.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			int fieldValue = field.getInt(object);
			if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
				value = fieldValue;
			}
		} catch (Exception e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}

		return value;
	}
	
	
	/**
	 * 下载图片
	 */
	private void downloadPhoto(final ImageView imageview,
			final String path) {
		
			Bitmap bitmap = getBitmap(path);
			if (bitmap!=null) {
				// 3.吧图片加入到缓存
				addBitmapToLruCache(path, bitmap);
				refreashBitmap(path, imageview, bitmap);
			}
			
//		new AsyncTask<String, Void, Bitmap>() {
//
//			@Override
//			protected Bitmap doInBackground(String... params) {
//				return getBitmap(path);
//			}
//
//			@Override
//			protected void onPostExecute(Bitmap result) {
//				super.onPostExecute(result);
//				if (result != null && imageview != null) {
//					// 3.吧图片加入到缓存
//					addBitmapToLruCache(path, result);
//					refreashBitmap(path, imageview, result);
//				} else {
//					Log.e(TAG, "在downloadAsyncTask里异步加载图片失败！");
//				}
//			}
//			
//		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] {});

	}
	
	/**
	 * 通过该网络路径获取Bitmap
	 *
	 * @param urlPath  该图片的网络路径
	 */
	private Bitmap getBitmap(String urlPath) {
		Bitmap bitmap = null;
		try {
			String fullName = PATH+getLastName(urlPath);
			if (judgeExists(fullName)) {/* 存在就直接使用 */
				bitmap = revitionImageSize(fullName);
			} else {/* 去下载图片,下载完成之后返回该对象 */
				bitmap = downloadAndSaveBitmap(urlPath, fullName);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return bitmap;
	}
	/**
	 * 获取最后的‘/’后的文件名
	 * 
	 * @param name
	 * @return
	 */
	private static String getLastName(String name) {
		if (TextUtils.isEmpty(name)) {
			return "";
		}
		int lastIndexOf = 0;
		try {
			lastIndexOf = name.lastIndexOf('/');
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return !name.equals("") ? name.substring(lastIndexOf + 1) + "1" : "";
	}
	/**
	 * 判断文件是否存在
	 * 
	 * @param fullName 文件在本地的完整名
	 * @return
	 */
	private static boolean judgeExists(String fullName) {

		try {
			File file = new File(fullName);
			return file.exists();
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * 从本地制作图片（Bitmap）
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static Bitmap revitionImageSize(String path) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(
				new File(path)));
		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(in, null, options);
		in.close();
		int i = 0;
		Bitmap bitmap = null;
		while (true) {
			if ((options.outWidth >> i <= 1000)
					&& (options.outHeight >> i <= 1000)) {
				in = new BufferedInputStream(
						new FileInputStream(new File(path)));
				options.inSampleSize = (int) Math.pow(2.0D, i);
				options.inJustDecodeBounds = false;
				bitmap = BitmapFactory.decodeStream(in, null, options);
				break;
			}
			i += 1;
		}
//		int orientation = Bimp.readPictureDegree(path);//获取旋转角度  
//		if(Math.abs(orientation) > 0){  
//			bitmap = Bimp.rotaingImageView(orientation, bitmap);//旋转图片  
//		} 
		return bitmap;
	}
	
	/**
	 * 下载保存图片
	 * 
	 * @param urlPath
	 *            下载路径
	 * @param fullName
	 *            文件保存路径+文件名
	 * @return
	 */
	private Bitmap downloadAndSaveBitmap(String urlPath, String fullName) {

		Bitmap bitmap = null;
		try {
			byte[] byteData = getImageByte(urlPath);
			if (byteData == null) {
				Log.e(TAG, "没有得到图片的byte，问题可能是path：" + urlPath);
				return null;
			}
			saveBitmap(fullName, byteData);
			bitmap = revitionImageSize(fullName);
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}

		return bitmap;
	}
	
	/**
	 * 获取图片的byte数组
	 * 
	 * @param urlPath
	 * @return
	 */
	private byte[] getImageByte(String urlPath) {
		InputStream in = null;
		byte[] result = null;
		try {
			int index = urlPath.lastIndexOf("/");
			String firstString = urlPath.substring(0, index + 1);
			String secondString = urlPath.substring(index + 1, urlPath.length());
			urlPath = firstString + Uri.encode(secondString);
			URL url = new URL(urlPath);
			HttpURLConnection httpURLconnection = (HttpURLConnection) url
					.openConnection();
			httpURLconnection.setConnectTimeout(10000);  
			httpURLconnection.setReadTimeout(30000);  
			httpURLconnection.setDoInput(true);
			httpURLconnection.connect();
			if (httpURLconnection.getResponseCode() == 200) {
				in = httpURLconnection.getInputStream();
				result = readInputStream(in, httpURLconnection.getContentLength());
				in.close();
			} else {
				Log.e(TAG, "下载图片失败，状态码是：" + httpURLconnection.getResponseCode());
			}
		} catch (Throwable e) {
			Log.e(TAG, "下载图片失败，原因是：" + e.toString());
			e.printStackTrace();
		} finally {
//			Log.e(TAG, "下载图片失败!");
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	/**
	 * 将输入流转为byte数组
	 * 
	 * @param in
	 * @param length 
	 * @return
	 * @throws Throwable
	 */
	private static byte[] readInputStream(InputStream in, int length) throws Exception {
//		int progress = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = -1;
		while ((len = in.read(buffer)) != -1) {
			baos.write(buffer, 0, len);
			try {
//				if (originalTask != null) {
//					progress += len;
//					originalTask.updateProgress(progress * 100 / length);
//				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		baos.close();
		in.close();
//		if (originalTask != null) {
//			originalTask.updateProgress(100);
//		}
		return baos.toByteArray();
	}
	/**
	 * 保存图片
	 * 
	 * @param fullName
	 * @param bitmap
	 */
	private void saveBitmap(String fullName, byte[] buffer) {
		try {
			File file = new File(fullName);
			if (!file.exists()) {
				creatFolder(fullName);
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(buffer);
			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "图片保存失败，异常信息是：" + e.toString());
		}
	}
	/**
	 * 创建保存文件的文件夹
	 * 
	 * @param fullName
	 *            带文件名的文件路径
	 * @return
	 */
	private static void creatFolder(String fullName) {
		if (getLastName(fullName).contains(".")) {
			String newFilePath = fullName.substring(0,
					fullName.lastIndexOf('/'));
			File file = new File(newFilePath);
			file.mkdirs();
		}
	}
}
