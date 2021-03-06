package com.fetal.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.fetal.R;
import com.fetal.base.ActivityBase;
import com.fetal.bean.MemberBean;
import com.fetal.util.DatabaseOperator;
import com.fetal.util.InputChecker;
import com.fetal.util.WebServer;
import com.fetal.activity.MemberActivity;

public class MemberActivity extends ActivityBase implements OnEditorActionListener{

	private InputMethodManager imm;
	private ConnectivityManager cm;
	private SharedPreferences sp;
	private EditText nickname, mobile;
	private TextView birthday;
	private ImageView camera, start;
	private Time time;
	private long current, date;
	private Handler handler;
	private ProgressDialog pdialog;
	private Bitmap thumbnail;
	private String[] members;
	private int total = 0;
	private final static int THUMBNAIL = 0, PHOTO = 1, CAMERA = 2, SWITCH = 3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_member);
		initTopUI();
		initUI();
	}
	
	/**
	 * 初始化UI
	 */
	private void initUI() {
		imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
		cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		top_title.setText("用户管理");
		top_left.setImageResource(R.drawable.btn_back);
		top_left.setVisibility(View.VISIBLE);
		top_left.setOnClickListener(this);
		top_right.setImageResource(R.drawable.btn_member_switch);
		top_right.setVisibility(View.VISIBLE);
		top_right.setOnClickListener(this);
		nickname = (EditText) findViewById(R.id.nickname);
		mobile = (EditText) findViewById(R.id.mobile);
		mobile.setInputType(EditorInfo.TYPE_CLASS_PHONE);
		mobile.setOnEditorActionListener(this);
		birthday = (TextView) findViewById(R.id.birthday);
		birthday.setOnClickListener(this);
		camera = (ImageView) findViewById(R.id.camera);
		camera.setOnClickListener(this);
		start = (ImageView) findViewById(R.id.start);
		start.setOnClickListener(this);
		time = new Time();
		time.setToNow();
		current = time.toMillis(false);
		sp = getSharedPreferences("user", MODE_PRIVATE);
		if (!getIntent().hasExtra("isnew")) {
			if (sp.contains("nickname")) {
				SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
				date = sp.getLong("birthday", 0);
				birthday.setText(format.format(new Date(date)));
				mobile.setText(sp.getString("mobile", ""));
				nickname.setText(sp.getString("nickname", ""));
				thumbnail = BitmapFactory.decodeFile("/sdcard/Fetal/" + sp.getInt("id", 0) + ".jpg");
				camera.setImageBitmap(thumbnail);
			}
		} else {
			thumbnail = null;
		}
		pdialog = new ProgressDialog(this);
		pdialog.setMessage("正在提交...");
		handler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				pdialog.dismiss();
				startActivity(new Intent(getBaseContext(), SettingActivity.class));
				finish();
			}
		};
	}
	
	/**
	 * 获取用户列表
	 */
	private void loadMemberList(){
		DatabaseOperator db = new DatabaseOperator(this);
		Cursor cursor = db.showAllMember();
		total = cursor.getCount();
		members = new String[total + 1];
		int i = 0;
		while (cursor.moveToNext()) {
			members[i++] = cursor.getString(cursor.getColumnIndex("nickname"));
		}
		members[i] = "新用户";
		db.closeDatabase();
	}
	
	@Override  
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getCurrentFocus() != null) {
                if (getCurrentFocus().getWindowToken() != null) {
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        }
        return super.onTouchEvent(event);
    }
	
	@SuppressLint("NewApi")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.camera:
				showDialog(THUMBNAIL);
				break;
			case R.id.top_left:
				finish();
				break;
			case R.id.top_right:
				loadMemberList();
				showDialog(SWITCH);
				break;
			case R.id.birthday:
		        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				Calendar calendar = Calendar.getInstance();
				DatePickerDialog.OnDateSetListener datelistener = new DatePickerDialog.OnDateSetListener() {
					
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear,
							int dayOfMonth) {
						// TODO Auto-generated method stub
						birthday.setText(year + " / " + (monthOfYear + 1) + " / " + dayOfMonth);
						time.set(dayOfMonth, monthOfYear, year);
						date = time.toMillis(false);
					}
				};
				DatePickerDialog dialog = new DatePickerDialog(
					this, 
					datelistener, 
					calendar.get(Calendar.YEAR), 
					calendar.get(Calendar.MONTH), 
					calendar.get(Calendar.DAY_OF_MONTH)
				);
				try {
					Field mDatePickerField = dialog.getClass().getDeclaredField("mDatePicker");
					mDatePickerField.setAccessible(true);
					DatePicker datePicker = (DatePicker) mDatePickerField.get(dialog);
		            datePicker.setMinDate(current);
		            datePicker.setMaxDate(current + 31536000000L);
					dialog.show();
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case R.id.start:
				//获取各值
				final String vbirthday = birthday.getText().toString().trim();
				final String vnickname = nickname.getText().toString().trim();
				final String vmobile = mobile.getText().toString().trim();
				String notice = "";
				//各项检测
				if (thumbnail == null) {
					notice += "请上传头像";
				}
				if (InputChecker.checkBirthday(vbirthday) == 1) {
					if (thumbnail == null) {
						notice += "\n\n";
					}
					notice += "请选择预产期";
				}
				switch (InputChecker.checkNickname(vnickname)) {
					case 1:
						if (!"".equals(notice)) notice += "\n\n";
						notice += "请填写昵称";
						break;
					case 2:
						if (!"".equals(notice)) notice += "\n\n";
						notice += "昵称长度不能超过16个字符";
						break;
				}
				switch (InputChecker.checkMobile(vmobile)) {
					case 1:
						if (!"".equals(notice)) notice += "\n\n";
						notice += "请填写手机号码";
						break;
					case 2:
						if (!"".equals(notice)) notice += "\n\n";
						notice += "请输入有效的手机号码";
						break;
				}
				//执行检测结果
				if ("".equals(notice)) {
					pdialog.show();
					Thread thread = new Thread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							//保存数据
							DatabaseOperator db = new DatabaseOperator(getBaseContext());
							MemberBean member = new MemberBean();
							member.setNickname(vnickname);
							member.setBirthday(date);
							member.setMobile(vmobile);
							int id = 0;
							if (getIntent().hasExtra("isnew")) {
								db.addMember(member);
								id = db.showAllMember().getCount();
								member.setRemoteId(0);
							} else {
								id = sp.getInt("id", 0);
								member.setId(id);
								db.modifyMember(member);
								member.setRemoteId(db.getMemberRemoteId(id));
							}
							Editor editor = sp.edit();
							editor.putInt("id", id);
							editor.putString("nickname", vnickname);
							editor.putLong("birthday", date);
							editor.putString("mobile", vmobile);
							editor.commit();
							try {
								if (checkProjectPath("Fetal")) {
									String filename = "/sdcard/Fetal/" + id + ".jpg";
									saveThumbnail(filename, thumbnail);
									member.setId(id);
									member.setThumbnail(filename);
									db.modifyMemberThumbnail(member);
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (cm.getActiveNetworkInfo().isAvailable()) {
								try {
									WebServer server = new WebServer();
									if (member.getRemoteId() == 0) {
										server.newMember(getBaseContext(), member);
									} else {
										server.modifyMember(member);
									}
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							db.closeDatabase();
							Message msg = Message.obtain();
							handler.sendMessage(msg);
						}
					});
					thread.start();
				} else {
					Toast.makeText(this, notice, Toast.LENGTH_SHORT).show();
				}
				break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case PHOTO:
				//获取图片路径
				Uri uri = data.getData();
				String[] filePathColumn = {MediaStore.Images.Media.DATA};
		        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
		        cursor.moveToFirst();
		        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		        String path = cursor.getString(columnIndex);
		        cursor.close();
		        //生成缩略图
				Bitmap pbitmap = BitmapFactory.decodeFile(path);
				int pw = pbitmap.getWidth();
				int ph = pbitmap.getHeight();
				int baseSize = camera.getWidth();
				if (pw > ph) {
					Bitmap.createBitmap(pbitmap, (pw - ph) / 2, 0, ph, ph);
				} else {
					Bitmap.createBitmap(pbitmap, 0, (ph - pw) / 2, pw, pw);
				}
				thumbnail = ThumbnailUtils.extractThumbnail(pbitmap, baseSize, baseSize);
				camera.setImageBitmap(thumbnail);
				break;
			case CAMERA:
				Bundle bundle = data.getExtras();
				Bitmap cbitmap = (Bitmap) bundle.get("data");
				int cw = cbitmap.getWidth();
				int ch = cbitmap.getHeight();
				if (cw > ch) {
					thumbnail = Bitmap.createBitmap(cbitmap, (cw - ch) / 2, 0, ch, ch);
					camera.setImageBitmap(thumbnail);
				} else {
					thumbnail = Bitmap.createBitmap(cbitmap, 0, (ch - cw) / 2, cw, cw);
					camera.setImageBitmap(thumbnail);
				}
				break;
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
			case THUMBNAIL:
				String[] from = new String[]{"本地图片", "马上拍照"};
				Builder cbuilder = new android.app.AlertDialog.Builder(this);
				cbuilder.setTitle("选择头像来源");
				cbuilder.setItems(from, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						switch (which) {
							case 0:
								startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), PHOTO);
								break;
							case 1:
								startActivityForResult(new Intent("android.media.action.IMAGE_CAPTURE"), CAMERA);
								break;
						}
					}
				});
				dialog = cbuilder.create();
				break;
			case SWITCH:
				Builder mbuilder = new android.app.AlertDialog.Builder(this);
				mbuilder.setTitle("用户列表");
				mbuilder.setItems(members, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if (total == which) {
							Intent intent = new Intent(MemberActivity.this, MemberActivity.class);
							intent.putExtra("isnew", true);
							startActivity(intent);
						} else {
							DatabaseOperator db = new DatabaseOperator(getBaseContext());
							MemberBean member = db.getOneMember(which + 1);
							Editor editor = sp.edit();
							editor.putInt("id", member.getId());
							editor.putString("nickname", member.getNickname());
							editor.putLong("birthday", member.getBirthday());
							editor.putString("mobile", member.getMobile());
							editor.commit();
							db.closeDatabase();
							startActivity(new Intent(MemberActivity.this, SettingActivity.class));
							finish();
						}
					}
				});
				dialog = mbuilder.create();
				break;
		}
		return dialog;
	}
	
	/**
	 * 保存头像
	 * @param bitmap
	 * @throws IOException
	 */
	public void saveThumbnail(String filename, Bitmap bitmap) throws IOException {
		File file = new File(filename);
		file.createNewFile();
		FileOutputStream out = null;
		out = new FileOutputStream(file);
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		out.flush();
		out.close();
		out = null;
		file = null;
		bitmap = null;
	}
	
	/**
	 * 生成项目文件夹
	 * @param filename 文件名称
	 */
	public boolean checkProjectPath(String path) {
		try {
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				String sdcard = Environment.getExternalStorageDirectory().getPath();
				path = sdcard + "/" + path;
				File dir = new File(path);
				if (!dir.exists()) {
					dir.mkdir();
				}
				dir = null;
				return true;
			} else {
				Log.e("failed", "create dir failed");
				return false;
			}
		} catch (Exception e) {
			// TODO: handle exception
			return false;
		}
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		// TODO Auto-generated method stub
		if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
			imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
		return false;
	}
}
