package com.example.myapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.app.Fragment;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapp.database.MyDao;
import com.example.myapp.self.Music;
import com.example.myapp.self.MyLogin;
import com.example.myapp.self.SelfFinal;
import com.example.myapp.self.SongListBean;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyAdapter extends BaseAdapter {

    private List<Music> mList;
    private LayoutInflater inflater;
    private Fragment fragment;
    private  static SQLiteDatabase db;
    private Activity context;
    private MyDao myDao;
    //正在播放的歌曲在列表中的位置
    private int nowPosition;

    private static final String local_stable = "local_music_list";
    private static final String near_stable = "near_music_list";
    private static final String download_stable = "download_music_list";
    private static final String love_stable = "love_music_list";
    private int index=-1;
    public MyAdapter(Activity context,List<Music> list ,Fragment fragment){
        this.inflater = LayoutInflater.from(context);
        this.mList = list;
        this.context = context;
        this.fragment = fragment;
        myDao=new MyDao(context);
    }
    private SQLiteDatabase getSQLiteDB(){
        return context.openOrCreateDatabase("mydb.db", Context.MODE_PRIVATE,null);
    }

    /**
     * 获取一个数据库当中是否包含正在操作的歌曲
     * @param music 正在操作的歌曲对象
     * @param tableName 要查询的数据库
     * @return  -1，数据库中不包含这个歌曲；0，包含，但该歌曲不是我喜欢的；1，这个歌曲在，并且是我喜欢的
     */
    private int selectByPath(Music music,String tableName){
        db = myDao.newDB();
        Cursor cursor=db.query(tableName,new String[]{"love"},"path=?",new String[]{music.getPath()},null,null,null);
        int love=0;
        if(cursor.moveToNext()){
            love = cursor.getInt(cursor.getColumnIndexOrThrow("love"));
        }else {
            love = -1;
        }
        if(!cursor.isClosed()){
            cursor.close();
        }
        return love;
    }

    private void updatelocalLove(Music music,String tableName,int loved){
        db = myDao.newDB();
        ContentValues values=new ContentValues();
        values.put("love",loved);
        db.update(tableName,values,"path=?",new String[]{music.getPath()});
    }

    //修改数据库love的值为1
    private void updateAllLove(Music music,int loved){
        //把本地歌单love修改为1
        updatelocalLove(music,local_stable,loved);
        updatelocalLove(music,near_stable,loved);
        updatelocalLove(music,download_stable,loved);
        updatelocalLove(music,love_stable,loved);
        //把自定义歌单love改为1
        updatelocalLove(music,"self_music_list",loved);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private int []images=new int[]{R.mipmap.ic_heart_48,R.mipmap.ic_heart_red_48};
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        if(convertView ==null){
            convertView = inflater.inflate(R.layout.song,null);
        }
        final Music m = mList.get(position);
        TextView name = (TextView) convertView.findViewById(R.id.songName);
        TextView author = (TextView)convertView.findViewById(R.id.songAuthor);
        final ImageButton loveBtn = (ImageButton)convertView.findViewById(R.id.loveImageButton);
        final ImageButton moreBtn = (ImageButton)convertView.findViewById(R.id.moreImageButton);
        //id等于-1000的是当前正在播放的歌曲对象，设置他的颜色为绿色，出现了第一屏正常显示，滑动之后出现每隔几行
        //字体颜色也为绿色的问题，主要原因是使用了convertView，这个是实现了缓存的
        //对于之前的多个歌曲都是显示绿色重复的问题，原因是ListView的复用问题，没有设置id不等于-1000的行的颜色，复用之前的
        //行就可能会出现复用了刚好是绿色的哪一个View，所以出现多个“正在播放”
        //在第一屏之后会复用前面已经渲染的View，之前在这个if语句中没有加入else条件，造成滑动后颜色重复问题
        //对于解决这个问题，就是对所有的条件重写，解决复用的问题，加入else条件即可
        if(m.getId()==-1000){
            if(m.getFlag()!=-1) {
                name.setTextColor(context.getResources().getColor(R.color.beautiful));
                author.setTextColor(context.getResources().getColor(R.color.beautiful));
            }else {
//                holder.flagBtn.setImageResource(R.mipmap.ic_phone_20);
                name.setTextColor(context.getResources().getColor(R.color.gray));
               author.setTextColor(context.getResources().getColor(R.color.gray));
            }
        }else {
            if(m.getFlag()!=-1) {
                name.setTextColor(context.getResources().getColor(R.color.color1));
                author.setTextColor(context.getResources().getColor(R.color.black));
            }else {
                name.setTextColor(context.getResources().getColor(R.color.gray));
                author.setTextColor(context.getResources().getColor(R.color.gray));
            }
        }
        if(m.getId() == -1){
            if(m.getFlag()!=-1) {
                name.setTextColor(context.getResources().getColor(R.color.color1));
                author.setTextColor(context.getResources().getColor(R.color.black));
            }else {
                name.setTextColor(context.getResources().getColor(R.color.gray));
                author.setTextColor(context.getResources().getColor(R.color.gray));
            }
        }
        //在我喜欢的音乐列表中隐藏按钮
        if(fragment instanceof LoveMusicFragment||fragment instanceof NearPlayListFragment||fragment instanceof DownloadMusicFragment){
            loveBtn.setVisibility(View.GONE);
        }
        loveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkNet(context)) {
                    if (!MyLogin.logined) {
                        //如果没有用户登录，直接跳转到用户登录界面
                        Intent intents = new Intent(context, Login_in.class);
                        context.startActivity(intents);
                    }else {
                        SongListBean bean=new SongListBean(MyLogin.loveId,null,0);
//                        if (fragment instanceof LocalMusicListFragment) {
                           int j = selectByPath(m,local_stable);
                        System.out.println("j="+j+"---------------------");
                        if(j == 1){
                            m.setLove(0);
                            myDao.deleteMusic(m,love_stable);
                            loveBtn.setImageResource(images[0]);
                            updateAllLove(m,0);
                            syncNetDelMusicFromSongList(m,bean);
                        }else {
                            m.setLove(1);
                            myDao.insertMusic(m,love_stable);
                            loveBtn.setImageResource(images[1]);
                            updateAllLove(m,1);
                            syncSongList(m,bean);

//                           }
                        }
                        Intent intent = new Intent("update_service_love");
                        intent.putExtra("loved",j);
                        intent.putExtra("music", m);
                        context.sendBroadcast(intent);
                    }
                }else {
                    Toast.makeText(context,"网络无法连接",Toast.LENGTH_LONG).show();
                }
            }
        });
        loveBtn.setImageResource(images[m.getLove()]);
        name.setText(m.getSongName());
        author.setText(m.getSongAuthor());
        final View finalConvertView = convertView;
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View view = LayoutInflater.from(context).inflate(R.layout.music_list_menu, null);
                final PopupWindow popupWindow = new PopupWindow(view, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setFocusable(true);
                popupWindow.setTouchable(true);
                popupWindow.setBackgroundDrawable(new ColorDrawable(0x000000));
                popupWindow.setOutsideTouchable(true);
                //设置弹出窗口背景变半透明，来高亮弹出窗口
                WindowManager.LayoutParams lp = context.getWindow().getAttributes();
                lp.alpha = 0.5f;
                context.getWindow().setAttributes(lp);

                popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        //恢复透明度
                        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
                        lp.alpha = 1f;
                        context.getWindow().setAttributes(lp);
                    }
                });
                popupWindow.showAtLocation(finalConvertView.findViewById(R.id.moreImageButton), Gravity.BOTTOM, 0, 0);
                TextView nameAuthor = (TextView) view.findViewById(R.id.name_author);
                nameAuthor.setText(m.getSongName() + " - " + m.getSongAuthor());
                final Button deleteBtn = (Button) view.findViewById(R.id.delete_btn);
                Button addBtn = (Button) view.findViewById(R.id.add_music_to_list);
                addBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                        showAddListWindow(context, moreBtn, m);
                    }
                });


                Button downloadBtn = (Button)view.findViewById(R.id.local_download_btn);
                if(fragment instanceof LocalMusicListFragment||m.getFlag()!=1){
                    downloadBtn.setEnabled(false);
                }else {
                    downloadBtn.setEnabled(true);
                    List<Music> mList = myDao.findAll("local_music_list");
                    //查找本地歌曲有没有这首歌
                    for (Music ms:mList){
                        if(ms.getSongName().equals(m.getSongName())&&ms.getSongAuthor().equals(m.getSongAuthor())){
                            downloadBtn.setEnabled(false);
                            break;
                        }
                    }
                }
                downloadBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent=new Intent("download_music");
                        intent.putExtra("music",m);
                        context.sendBroadcast(intent);
                        popupWindow.dismiss();
                    }
                });
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (fragment instanceof LocalMusicListFragment) {
                            popupWindow.dismiss();
                            index = -1;
                            String[] arr = {"同时删除本地歌曲文件"};
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("确定删除歌曲?");
                            builder.setSingleChoiceItems(arr, index, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    index = which;
                                }
                            });
                            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (index == 0) {
                                        myDao.deleteMusic(m, local_stable);
                                        mList.remove(position);
                                        notifyDataSetChanged();
                                        Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                        File file = new File(m.getPath());
                                        if (file.exists()) {
                                            file.delete();
                                        }
                                        File file1 = new File(m.getPath().split("\\.")[0] + ".lrc");
                                        if (file1.exists()) {
                                            file1.delete();
                                        }
                                        myDao.setFlagWithDeleteMusic(m, -1);
                                    } else {
                                        myDao.deleteMusic(m, local_stable);
                                        mList.remove(position);
                                        notifyDataSetChanged();
                                    }
                                }
                            }).setNegativeButton("取消", null);
                            builder.create().show();
                        } else if (fragment instanceof NearPlayListFragment) {
                            myDao.deleteMusic(m, near_stable);
                            popupWindow.dismiss();
                            mList.remove(position);
                            notifyDataSetChanged();
                            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                        } else if (fragment instanceof DownloadMusicFragment) {
                            myDao.deleteMusic(m, download_stable);
                            popupWindow.dismiss();
                            mList.remove(position);
                            notifyDataSetChanged();
                            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                        } else {
                            popupWindow.dismiss();
                            if (checkNet(context)) {
                                if (!MyLogin.logined) {
                                    Intent intent = new Intent(context, Login_in.class);
                                    context.startActivity(intent);
                                } else {
                                    myDao.deleteMusic(m, love_stable);
                                    //同时删除网络歌曲
                                    syncNetDelMusicFromSongList(m, new SongListBean(MyLogin.loveId, null, 0));
                                    updateAllLove(m, 0);
                                    mList.remove(position);
                                    notifyDataSetChanged();
                                    Intent intent = new Intent("update_service_love");
                                    intent.putExtra("music", m);
                                    context.sendBroadcast(intent);
                                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(context, "未连接到网络", Toast.LENGTH_SHORT).show();

                            }
                        }
                    }
                });
                Button cancelBtn = (Button) view.findViewById(R.id.cancelBtn);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });
                Button aboutBtn = (Button) view.findViewById(R.id.about_btn);
                aboutBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                        View mv = LayoutInflater.from(context).inflate(R.layout.music_about, null);
                        final PopupWindow pw = showPopupWindow(mv, view.findViewById(R.id.about_btn));
                        TextView name = (TextView) mv.findViewById(R.id.about_song_name);
                        name.setText("歌名：" + m.getSongName());
                        TextView author = (TextView) mv.findViewById(R.id.about_song_author);
                        author.setText("歌手：" + m.getSongAuthor());
                        TextView time = (TextView) mv.findViewById(R.id.about_song_all_time);
                        time.setText("时长：" + transforTime(m.getAlltime()));
                        TextView size = (TextView) mv.findViewById(R.id.about_song_size);
                        double sizes = (double) m.getSongSize() / (1024 * 1024);
                        Log.i("文件大小:", sizes + "");
                        DecimalFormat format = new DecimalFormat("#.00");
                        size.setText("大小：" + format.format(sizes) + "M");
                        TextView path = (TextView) mv.findViewById(R.id.about_song_path);
                        path.setText("路径：" + m.getPath());
                        Button sureBtn = (Button) mv.findViewById(R.id.sureBtn);
                        sureBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                pw.dismiss();
                            }
                        });


                    }
                });
            }
        });
        ImageButton flagBtn = (ImageButton)convertView.findViewById(R.id.music_flag_btn);
        if(m.getFlag()==1){
            flagBtn.setImageResource(R.mipmap.ic_cloud_20);
        }else{
            flagBtn.setImageResource(R.mipmap.ic_phone_20);
        }
        return convertView;
    }

    private void showAddListWindow(final Activity context, View p, final Music music){
        if(!checkNet(context)) {
            Toast.makeText(context,"未连接到网络",Toast.LENGTH_LONG).show();
            return;
        }else {
            if (!MyLogin.logined) {
                Intent intent = new Intent(context, Login_in.class);
                context.startActivity(intent);
                return;
            }
        }
        List<SongListBean> songListBeanList=MyLogin.bean.getSongList();
        View views = LayoutInflater.from(context).inflate(R.layout.add_song_list,null);
        LinearLayout body_layout = (LinearLayout)views.findViewById(R.id.song_list_body);
        final PopupWindow popupWindow=new PopupWindow(views,WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
        for(final SongListBean bean:songListBeanList){
            View view1=LayoutInflater.from(context).inflate(R.layout.button_layout,null);
            Button button=(Button)view1.findViewById(R.id.self_button);
            button.setText(bean.getListName().toLowerCase());
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //获得这个歌单的所有歌曲
                    List<Music> allMusic;
                    if(bean.getListId()==MyLogin.loveId){
                        allMusic = myDao.findAll("love_music_list");
                    }else {
                        allMusic = myDao.findAll(bean.getListId(),"self_music_list");
                    }
                    int i=0;
                    for(i=0;i<allMusic.size();i++){
                        Music mm = allMusic.get(i);
                        if(mm.getSongName().equals(music.getSongName())&&mm.getSongAuthor().equals(music.getSongAuthor())){
                            //歌曲已经在这个歌单中
                            break;
                        }
                    }
                    if(i>=allMusic.size()) {
                        int listId = bean.getListId();
                        //将这个歌曲加入到本地数据库
                        long iii=0;
                        if(bean.getListId()==MyLogin.loveId){
                            iii=myDao.insertMusic(music,love_stable);
//                            updateLove(music,local_stable);
                            music.setLove(1);
                            updateAllLove(music,1);
                            notifyDataSetChanged();

                        }else {
                            iii = myDao.insertMusic(listId, music, "self_music_list");
                        }
                        if(iii>0) {
                            Log.i("歌曲" + music.getSongName(), "已加入到数据库中");
                            //在把这个歌曲同步到服务器
                            syncSongList(music, bean);
                            Toast.makeText(context, "歌曲已加入歌单中", Toast.LENGTH_LONG).show();
                        }
                    }else {
                        Toast.makeText(context,"已存在",Toast.LENGTH_LONG).show();
                    }
                    popupWindow.dismiss();
                }
            });
            body_layout.addView(view1);
        }
        popupWindow.setFocusable(true);
        popupWindow.setTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x000000));
        popupWindow.setOutsideTouchable(true);
        //设置弹出窗口背景变半透明，来高亮弹出窗口
        WindowManager.LayoutParams lp =context.getWindow().getAttributes();
        lp.alpha=0.5f;
        context.getWindow().setAttributes(lp);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //恢复透明度
                WindowManager.LayoutParams lp =context.getWindow().getAttributes();
                lp.alpha=1f;
                context.getWindow().setAttributes(lp);
            }
        });
        popupWindow.showAtLocation(p,Gravity.BOTTOM,0,0);
    }


    private void syncSongList(Music m, SongListBean bean) {
        OkHttpClient client = new OkHttpClient();
        //用户id
        int userId = MyLogin.bean.getId();
        int listId = bean.getListId();
        int mId = m.getFlag();
        RequestBody body = new FormBody.Builder().add("user_id", String.valueOf(userId))
                .add("song_list_id", String.valueOf(listId))
                .add("music_id", String.valueOf(mId))
                .add("music_name", m.getSongName())
                .add("music_author", m.getSongAuthor())
                .add("music_path", m.getPath()).build();
        String url =  SelfFinal.host+SelfFinal.port+ "/music/user/syncAddMusic";
        String urls =  SelfFinal.host+SelfFinal.port+ "/music/user/syncAddMusic";
        Request request = new Request.Builder().post(body).url(urls).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("上传新的歌曲","成功");
            }
        });
    }
    private void syncNetDelMusicFromSongList(Music music,SongListBean bean){
        OkHttpClient client=new OkHttpClient();
        int listId = bean.getListId();
        String songName = music.getSongName();
        String songAuthor = music.getSongAuthor();
        RequestBody body=new FormBody.Builder().add("listId",String.valueOf(listId))
                .add("songName",songName)
                .add("songAuthor",songAuthor).build();
        String url = SelfFinal.host+SelfFinal.port+ "/music/user/syncDelMusic";
        String urls = SelfFinal.host+SelfFinal.port+"/music/user/syncDelMusic";
        Request request=new Request.Builder().url(urls).post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });

    }

    public boolean checkNet(Context context){
        if(context!=null){
            ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info=manager.getActiveNetworkInfo();
            if(info!=null){
                return info.isConnected();
            }
        }
        return false;
    }

    /**
     * 创建一个弹窗
     * @param view  弹出的窗口的view对象
     * @param finalConvertView    菜单按钮的view对象
     */
    private PopupWindow showPopupWindow(View view,View finalConvertView){
        PopupWindow popupWindow=new PopupWindow(view,WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT,true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        //设置弹出窗口背景变半透明，来高亮弹出窗口
        WindowManager.LayoutParams lp =context.getWindow().getAttributes();
        lp.alpha=0.5f;
        context.getWindow().setAttributes(lp);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //恢复透明度
                WindowManager.LayoutParams lp =context.getWindow().getAttributes();
                lp.alpha=1f;
                context.getWindow().setAttributes(lp);
            }
        });
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAtLocation(finalConvertView, Gravity.BOTTOM,0,0);
        return popupWindow;
    }

    /**
     * 把时间毫秒转化为分钟
     * @param time
     * @return
     */
    public String transforTime(long time){
        long million = time/1000;
        int mill = (int)million%60; //获取秒
        int minute = (int)million/60;
        String allTime = String.valueOf(minute)+":"+String.valueOf(mill);
        return allTime;
    }
}
