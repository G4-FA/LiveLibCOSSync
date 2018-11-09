import java.io.File;
import java.util.ArrayList;
import java.util.List;

class SyncUtilsPC {

	
	// 删除COS上的空文件夹
	// 本身COS没有文件夹 是对老习惯的模拟 实际是空对象 但是管理起来不稳定
	public List<tObjectInfo> getAllPCObjectList(String path) throws Exception {
		
    	System.out.println( "[SyncUtilsPC getAllPCObjectList]" );
    	
    	List<tObjectInfo> retList = new ArrayList<tObjectInfo>();
    	
    	traverseFolder(path,retList);
    	
    	return retList;
		
	}
	
	
	// 递归遍历文件夹
	private void traverseFolder(String path, List<tObjectInfo> retList) throws Exception {

        File file = new File(path);
        if ( !file.exists() ) { throw new Exception("traverseFolder path error"); }

        File[] files = file.listFiles();
        if (null == files || files.length == 0) { return; }

        for (File file2 : files) {
        	
            if (file2.isDirectory()) {
            	
            	System.out.printf( "Find PC Folder --- name:%s\n", file2.getAbsolutePath() );
                traverseFolder(file2.getAbsolutePath(), retList);

            } else {

            	String name = file2.getAbsolutePath();
            	long size = file2.length();
                tObjectInfo info = new tObjectInfo( PCFilename2COSKey(name), size, name, "PC-ETAG" );
                retList.add(info);
                System.out.printf( "Find PC Object --- name:%s size:%d\n", name, size );

            }
            
        }

    }
	
	// 将PC本地文件名路径转换为COSKey
	private String PCFilename2COSKey( String filename ) {
		
		String ret = filename.replace("\\", "/");
		ret = ret.substring( LiveLibCOSSync.PC_ROOT_DIR.length() );
		ret = LiveLibCOSSync.RootDir+ret;
		return ret;
	}
	
}
