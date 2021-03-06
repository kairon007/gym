package com.gym.ui.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.gym.R;
import com.gym.app.Constants;
import com.gym.bean.FitSpaceBean;
import com.gym.http.protocol.AddPublishLessonProtocol;
import com.gym.http.protocol.BaseProtocol;
import com.gym.http.protocol.GetFitSpaceProtocol;
import com.gym.ui.adapter.ShowAddressAdapter;
import com.gym.utils.ImageUtils;
import com.gym.utils.ProgressUtil;
import com.gym.utils.StringUtils;
import com.gym.utils.Tool;
import com.gym.utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Administrator on 2015/9/1 0001.
 */
public class AddPublishLessonActivity extends BaseActivity implements View.OnClickListener {
    @InjectView(R.id.title_tb)
    TextView titleTb;
    @InjectView(R.id.back_tb)
    ImageView backTb;
    @InjectView(R.id.area_tb)
    TextView areaTb;
    @InjectView(R.id.right_tv)
    TextView rightTv;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.name_tv)
    TextView nameTv;
    @InjectView(R.id.name)
    EditText name;
    @InjectView(R.id.gender_tv)
    TextView genderTv;
    @InjectView(R.id.addImage)
    ImageView addImage;
    @InjectView(R.id.height_tv)
    TextView heightTv;
    @InjectView(R.id.lesson_people)
    EditText lessonPeople;
    @InjectView(R.id.period_tv)
    TextView periodTv;
    @InjectView(R.id.lesson_period)
    TextView lessonPeriod;
    @InjectView(R.id.time_tv)
    TextView timeTv;
    @InjectView(R.id.lesson_time)
    TextView lessonTime;
    @InjectView(R.id.during_tv)
    TextView duringTv;
    @InjectView(R.id.lesson_during)
    EditText lessonDuring;
    @InjectView(R.id.lesson_intro)
    MultiAutoCompleteTextView lessonIntro;
    @InjectView(R.id.fit)
    TextView fit;
    @InjectView(R.id.fit_et)
    TextView fitEt;
    @InjectView(R.id.category)
    TextView category;
    @InjectView(R.id.root)
    LinearLayout root;
    private ActionBar mActionBar;
    private boolean loading = false;

    private View dialogView;
    private int TAKE_PICTURE = 100, PHOTO_ALBUM = 200;
    private int takeType = 1;
    private String imgPath1;
    private static final int CROP_PICTURE = 0;
    private String protraitPath1;
    String filePath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + Constants.APP_TMEP_FILE_PATH + "/";
    private Button takingPictures;
    private Button selectPphotoAlbum;
    private Button btnClose;
    private Dialog dialog;
    private ArrayList<FitSpaceBean> fitSpaces;
    private PopupWindow showPopup;
    private ShowAddressAdapter spaceAdapter;

    @Override
    public void init() {
        super.init();
        setContentView(R.layout.activity_input_lesson);
        ButterKnife.inject(this);
    }

    @Override
    public void initData() {
        super.initData();
        initOperationDialog();
        addImage.setOnClickListener(this);
        fitEt.setOnClickListener(this);
        category.setOnClickListener(this);

    }

    @Override
    public void initActionbar() {
        super.initActionbar();
        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setDefaultDisplayHomeAsUpEnabled(true);
        areaTb.setVisibility(View.GONE);
        backTb.setVisibility(View.VISIBLE);
        titleTb.setText(UIUtils.getString(R.string.publishLesson));
        rightTv.setVisibility(View.VISIBLE);
        rightTv.setText(UIUtils.getString(R.string.submit));
        operateItems();
    }

    private void operateItems() {
        rightTv.setOnClickListener(this);
        backTb.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == rightTv) {
            if (!loading)
                new AddPublishLessonTask().execute();
        } else if (view == selectPphotoAlbum) {// 从相册取
            openAlbum();
            dialog.dismiss();// 操作框 取消
        } else if (view == takingPictures) {// 拍照
            takePicture();
            dialog.dismiss();// 操作框 取消
        } else if (view == btnClose) {
            dialog.dismiss();// 操作框 取消s
        } else if (view == addImage) {
            showDialog();
        } else if (view == backTb) {
            onBackPressed();
        } else if (view == fitEt) {
            if (fitSpaces != null)
                fitSpaces.clear();
            new FFListTask().execute();

        } else if (view == category) {
            if (fitSpaces != null) {
                fitSpaces.clear();
            } else {
                fitSpaces = new ArrayList<>();
            }
            FitSpaceBean bean1 = new FitSpaceBean();
            bean1.setGymName("健身");
            FitSpaceBean bean2 = new FitSpaceBean();
            bean2.setGymName("球类");
            FitSpaceBean bean3 = new FitSpaceBean();
            bean3.setGymName("格斗");
            fitSpaces.add(bean1);
            fitSpaces.add(bean2);
            fitSpaces.add(bean3);
            showPopup(category);
        }
    }

    private void showPopup(final TextView et) {
        showPopup = new PopupWindow(this);
        // 设置宽度
        showPopup.setWidth(name.getWidth());
        showPopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        // 设置焦点
        showPopup.setFocusable(true);
        // TODO
        ListView popupList = new ListView(this);
        popupList.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        popupList.setPadding(20, 20, 20, 20);
        spaceAdapter = new ShowAddressAdapter(fitSpaces, this);
        popupList.setAdapter(spaceAdapter);
        showPopup.setContentView(popupList);
        popupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                et.setText(fitSpaces.get(i).getGymName());
                showPopup.dismiss();
            }
        });
        // 把popupWindow显示出来
        if (!showPopup.isShowing()) {
            int[] xy = calcPopupXY(popupList, root);
            //不用任何gravity，使用绝对的(x,y)坐标
            showPopup.showAtLocation(root, Gravity.CENTER, xy[0], xy[1]);
        }

    }

    private int[] calcPopupXY(View rootView, View anchor) {
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        rootView.measure(w, h);
        int popupWidth = rootView.getMeasuredWidth();
        int popupHeight = rootView.getMeasuredHeight();
        Rect anchorRect = getViewAbsoluteLocation(anchor);
        Rect parentRect = getViewAbsoluteLocation(root);
        // int x = anchorRect.left + (anchorRect.right - anchorRect.left)/2 - popupWidth / 2;
        // int y = anchorRect.top - popupHeight;
        int x = anchorRect.left;
        int y = parentRect.top - popupHeight;
        return new int[]{x, y};
    }

    public static Rect getViewAbsoluteLocation(View view) {
        if (view == null) {
            return new Rect();
        }
        // 获取View相对于屏幕的坐标
        int[] location = new int[2];
        view.getLocationOnScreen(location);//这是获取相对于屏幕的绝对坐标，而view.getLocationInWindow(location); 是获取window上的相对坐标，本例中只有一个window，二者等价
        // 获取View的宽高
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        // 获取View的Rect
        Rect rect = new Rect();
        rect.left = location[0];
        rect.top = location[1];
        rect.right = rect.left + width;
        rect.bottom = rect.top + height;
        return rect;
    }

    public HashMap<String, String> getParams() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("coachID", String.valueOf(Constants.user.getUsr_UserID()));
        hashMap.put("courseTitle", name.getText().toString());
        String topicContent = Base64.encodeToString(Tool.bitmapTobyte(Tool.getSmallBitmap(protraitPath1)), Base64.DEFAULT).replace("\n", "");
        hashMap.put("img", topicContent);
        hashMap.put("courseIntroduce", lessonIntro.getText().toString());
        hashMap.put("peopleCount", lessonPeople.getText().toString());
        hashMap.put("CourseTime", lessonPeriod.getText().toString());
        hashMap.put("CourseCycle", lessonTime.getText().toString());
        hashMap.put("MaintenanceCycle", lessonDuring.getText().toString());
        return hashMap;
    }


    class AddPublishLessonTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading = true;
            ProgressUtil.startProgressBar(AddPublishLessonActivity.this);
        }

        @Override
        protected String doInBackground(String... strings) {
            HashMap<String, String> hashMap = getParams();
            AddPublishLessonProtocol protocol = new AddPublishLessonProtocol(hashMap);
            return protocol.load(UIUtils.getString(R.string.AddNewCourse_URL), BaseProtocol.POST);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            loading = false;
            ProgressUtil.stopProgressBar();
            if (TextUtils.isEmpty(s)) {
                UIUtils.showToastSafe(AddPublishLessonActivity.this, UIUtils.getString(R.string.network_error));
            } else {
                UIUtils.showToastSafe(AddPublishLessonActivity.this, s);
            }
        }
    }

    private void initOperationDialog() {
        dialogView = this.getLayoutInflater().inflate(
                R.layout.activity_edituserinfo_popuoperat, null);
        dialog = new Dialog(this, R.style.transparentFrameWindowStyle);
        takingPictures = (Button) dialogView.findViewById(R.id.takingPictures);
        selectPphotoAlbum = (Button) dialogView.findViewById(R.id.selectPphotoAlbum);
        btnClose = (Button) dialogView.findViewById(R.id.btnClose);
        takingPictures.setOnClickListener(this);
        selectPphotoAlbum.setOnClickListener(this);
        btnClose.setOnClickListener(this);
    }

    private void showDialog() {

        dialog.setContentView(dialogView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Window window = dialog.getWindow();
        window.setWindowAnimations(R.style.operatPopMenu_animstyle);
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.x = 0;
        wl.y = this.getWindowManager().getDefaultDisplay().getHeight();
        // 以下这两句是为了保证按钮可以水平满屏
        wl.width = ViewGroup.LayoutParams.MATCH_PARENT;
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        dialog.onWindowAttributesChanged(wl);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    } // 打开本地相册

    public void openAlbum() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        this.startActivityForResult(intent, PHOTO_ALBUM);
    } // 拍照

    public void takePicture() {

        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File outDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            File outFile = new File(outDir, System.currentTimeMillis() + ".jpg");
            if (takeType == 1) {
                imgPath1 = outFile.getAbsolutePath();
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outFile));
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            startActivityForResult(intent, TAKE_PICTURE);
        } else {
            UIUtils.showToastSafe(this, "手机设备无存储SDCard,请确认已经插入SD卡.");
        }
    }

    private void startPhotoZoom(Uri data, int takeType) {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(data, "image/*");
        intent.putExtra("output", this.getUploadTempFile(data, takeType));
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);// 裁剪框比例
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 600);// 输出图片大小
        intent.putExtra("outputY", 600);

        intent.putExtra("scale", true);// 去黑边
        intent.putExtra("scaleUpIfNeeded", true);// 去黑边
        startActivityForResult(intent, this.CROP_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE) {
            if (resultCode == RESULT_OK) {
                if (takeType == 1) {
                    //setImageView(imgPath1);
                    File file = new File(imgPath1);
                    startPhotoZoom(Uri.fromFile(file), 1);
                }
            } else if (resultCode == RESULT_CANCELED) {
                // 用户取消了图像捕获
            } else {
                // 图像捕获失败，提示用户
                UIUtils.showToastSafe(this, "拍照失败");
            }
        } else if (requestCode == PHOTO_ALBUM) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    String realPath = getRealPathFromURI(uri);
                    //setImageView(realPath);
                    startPhotoZoom(uri, takeType);
                    if (takeType == 1) {
                        imgPath1 = realPath;
                    }
                } else {
                    UIUtils.showToastSafe(this, "从相册获取图片失败");
                }
            }
        } else if (requestCode == CROP_PICTURE) {
            if (resultCode == RESULT_OK) {
                // uploadNewPhoto();// 上传新照片
                String realPath = null;
                if (takeType == 1) {
                    realPath = protraitPath1;
                }
                setImageView(realPath);
            }
        }
    }

    private Uri getUploadTempFile(Uri uri, int takeType) {
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            File savedir = new File(filePath);
            if (!savedir.exists()) {
                savedir.mkdirs();
            }
        } else {

            return null;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date());
        String thePath = ImageUtils.getAbsolutePathFromNoStandardUri(uri);

        // 如果是标准Uri
        if (StringUtils.isEmpty(thePath)) {
            thePath = ImageUtils.getAbsoluteImagePath(this, uri);
        }
        String ext = "jpg";
        ext = StringUtils.isEmpty(ext) ? "jpg" : ext;
        // 照片命名
        String cropFileName = "super_crop_" + timeStamp + "." + ext;
        // 裁剪头像的绝对路径
        File protraitFile = null;
        if (takeType == 1) {
            protraitPath1 = filePath + cropFileName;
            protraitFile = new File(protraitPath1);

            Uri cropUri = Uri.fromFile(protraitFile);
        }
        Uri cropUri = Uri.fromFile(protraitFile);
        return cropUri;
    }

    public String getRealPathFromURI(Uri contentUri) {
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = this.managedQuery(contentUri, proj, null, null,
                    null);
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            return contentUri.getPath();
        }
    }

    private void setImageView(final String realPath) {

        Bitmap bmp = BitmapFactory.decodeFile(realPath);
        int degree = readPictureDegree(realPath);
        if (takeType == 1) {
            if (degree <= 0) {
                addImage.setImageBitmap(bmp);
            } else {
                Matrix matrix = new Matrix();
                // 旋转图片动作
                matrix.postRotate(degree);
                // 创建新图片
                Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0,
                        bmp.getWidth(), bmp.getHeight(), matrix, true);

                addImage.setImageBitmap(resizedBitmap);
            }
        }
    }

    /**
     * 读取照片exif信息中的旋转角度<br/>
     *
     * @param path 照片路径
     * @return角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    class FFListTask extends AsyncTask<String, String, ArrayList<FitSpaceBean>> {

        @Override
        protected ArrayList<FitSpaceBean> doInBackground(String... strings) {
            GetFitSpaceProtocol protocol = new GetFitSpaceProtocol();
            return protocol.load(UIUtils.getString(R.string.GetFFlist_URL), BaseProtocol.POST);
        }

        @Override
        protected void onPostExecute(ArrayList<FitSpaceBean> fitSpaceBeans) {
            super.onPostExecute(fitSpaceBeans);
            if (fitSpaceBeans != null) {
                fitSpaces = fitSpaceBeans;
                showPopup(fitEt);

            }
        }
    }
}
