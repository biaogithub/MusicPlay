package com.example.myapp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyNetAdapter extends BaseAdapter {
    private Activity context;
    private List<Music> musicList;
    private ViewHolder holder;
    private MyDao myDao;
    private static final String local_stable = "local_music_list";
    private static final String near_stable = "near_music_list";
    private static final String download_stable = "download_music_list";
    private static final String love_stable = "love_music_list";
    public MyNetAdapter(Activity context,List<Music> list){
        this.context=context;
        this.musicList=list;
        myDao = new MyDao(this.context);
    }


    private void updatelocalLove(Music music,String tableName,int loved){
        ContentValues values=new ContentValues();
        values.put("love",loved);
        myDao.newDB().update(tableName,values,"path=?",new String[]{music.getPath()});
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
        return musicList.size();
    }

    @Override
    public Object getItem(int position) {
        return musicList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    private class ViewHolder{
        TextView songName;
        TextView songAuthor;
        ImageButton menuMusic;
        ImageButton flagBtn;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.net_item_music, null);
            holder = new ViewHolder();
            holder.songName = (TextView) convertView.findViewById(R.id.song_name_net);
            holder.songAuthor = (TextView) convertView.findViewById(R.id.song_author_net);
            holder.menuMusic = (ImageButton) convertView.findViewById(R.id.menu_music_net);
            holder.flagBtn = (ImageButton)convertView.findViewById(R.id.music_flag_btn);
            convertView.setTag(holder);
        }else{
            holder=(ViewHolder)convertView.getTag();
        }
        final Music music=musicList.get(position);
        holder.songName.setText(music.getSongName());
        holder.songAuthor.setText(music.getSongAuthor());
        final View finalConvertView = convertView;
        holder.menuMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View myView = LayoutInflater.from(context).inflate(R.layout.net_music_menu,null);
                final PopupWindow popupWindow=new PopupWindow(myView, WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
                popupWindow.setFocusable(true);
                popupWindow.setTouchable(true);
                popupWindow.setOutsideTouchable(true);
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
                popupWindow.showAtLocation(finalConvertView.findViewById(R.id.menu_music_net), Gravity.BOTTOM,0,0);
                TextView songNameAndAuthor = (TextView)myView.findViewById(R.id.on_song_about);
                songNameAndAuthor.setText("歌曲："+music.getSongName()+" - "+music.getSongAuthor());
                final Button comeToLove= (Button)myView.findViewById(R.id.come_to_love);
                comeToLove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                        showAddListWindow(context,comeToLove,music);
                    }
                });
                Button cancel = (Button)myView.findViewById(R.id.cancel_btn_net);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });
                Button downloadBtn = (Button)myView.findViewById(R.id.download_music);
                List<Music> mList = myDao.findAll("local_music_list");
                //查找本地歌曲有没有这首歌
                for (Music m:mList){
                    if(m.getSongName().equals(music.getSongName())&&m.getSongAuthor().equals(music.getSongAuthor())){
                        downloadBtn.setEnabled(false);
                        break;
                    }
                }
                downloadBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent=new Intent("download_music");
                        intent.putExtra("music",music);
                        context.sendBroadcast(intent);
                        popupWindow.dismiss();
                    }
                });
            }
        });
        if(music.getFlag()==0){
            holder.flagBtn.setImageResource(R.mipmap.ic_phone_20);
            holder.songName.setTextColor(context.getResources().getColor(R.color.color1));
            holder.songAuthor.setTextColor(context.getResources().getColor(R.color.color1));
        }else if(music.getFlag()==1){
            holder.flagBtn.setImageResource(R.mipmap.ic_cloud_20);
            holder.songName.setTextColor(context.getResources().getColor(R.color.color1));
            holder.songAuthor.setTextColor(context.getResources().getColor(R.color.color1));
        }else {
            holder.flagBtn.setImageResource(R.mipmap.ic_phone_20);
            holder.songName.setTextColor(context.getResources().getColor(R.color.gray));
            holder.songAuthor.setTextColor(context.getResources().getColor(R.color.gray));
        }
        return convertView;
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
    public void showAddListWindow(final Activity context, View p, final Music music){
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
            button.setText(bean.getListName());
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<Music> allMusic;
                    //获得这个歌单的所有歌曲
                    if(bean.getListId()==MyLogin.loveId) {
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
                        //歌曲不再这个歌单中
                        int listId = bean.getListId();
                        //将这个歌曲加入到本地数据库
                        long iii=0;
                        if(bean.getListId()==MyLogin.loveId){
                            music.setLove(1);
                            iii=myDao.insertMusic(music,"love_music_list");
                            updateAllLove(music,1);
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
        String url = SelfFinal.host+SelfFinal.port +"/music/user/syncAddMusic";
        Request request = new Request.Builder().post(body).url(url).build();
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
        SQLiteDatabase db = getSQLiteDB();
        Cursor cursor=db.query(tableName,new String[]{"love"},"path=?",new String[]{music.getPath()},null,null,null);
        int size = cursor.getCount();
        Log.i(tableName,"size = "+size);
        int love=0;
        if(size >0) {
            cursor.moveToFirst();
            love = cursor.getInt(cursor.getColumnIndexOrThrow("love"));
        }
        if(db.isOpen()) {
            db.close();
        }
        if(size==0){
            return -1;
        }
        Log.i(tableName,"love="+love);
        return love;
    }

    /**
     * 更新一个数据库中的love列，并且在喜欢和不喜欢之间自由转化
     * @param music 要更新的歌曲
     * @param tableName 数据库表名
     */
    private int updateLove(Music music,String tableName){
        SQLiteDatabase db = getSQLiteDB();
        int i=selectByPath(music,tableName);
        int j=0;
        if(i>-1) {
            j = i>0?0:1;
            Log.i(tableName,""+j);
            ContentValues values = new ContentValues();
            values.put("love",j);
            db.update(tableName,values,"path=?",new String[]{music.getPath()} );
        }
        if(db.isOpen()) {
            db.close();
        }
        return j;
    }
    //开启一个线程下载音乐
    public void downloadMusic(final Music music, final String url){
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client=new OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
                        .build();
                Request request=new Request.Builder().url(url).build();
                try {
                    Response response=client.newCall(request).execute();
                    if(response.isSuccessful()) {
                        Intent intent1=new Intent("what_download");
                        intent1.putExtra("music",music);
                        context.sendBroadcast(intent1);
                        InputStream inputStream = response.body().byteStream();
                        String name = response.header("Content-Disposition");
                        long size = response.body().contentLength();
                        String []strs = name.split("=");
                        String fileName="";
                        if(strs.length>1){
                            fileName = strs[1];
                        }
                        Log.i("文件名",fileName);
                        FileOutputStream out=new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath()+"/downloadMusic/"+fileName);
                        int i=0,j=0;
                        byte[] bytes=new byte[1024];
                        while ((i = inputStream.read(bytes))!=-1){
                            out.write(bytes,0,i);
                            j = j+i;
                            Intent intent=new Intent("update_dw_progress");
                            intent.putExtra("progress",(int)(j*100/size));
                            intent.putExtra("music",music);
                            context.sendBroadcast(intent);
                        }
                        Log.i("音乐"+fileName,"下载完毕");
                    }
                } catch (Exception e) {
                    if(e instanceof ConnectException){

                    }
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
