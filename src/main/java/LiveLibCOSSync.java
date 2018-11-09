	import java.util.List;
	import com.qcloud.cos.COSClient;
	import com.qcloud.cos.ClientConfig;
	import com.qcloud.cos.auth.BasicCOSCredentials;
	import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.region.Region;

class tObjectInfo
{
	tObjectInfo( String Key, long Size, String PCFilename, String Etag )
	{
		key = Key;
		size = Size;
		pcFilename = PCFilename;
		etag = Etag;
	}
	public String key;
	public long size;
	public String pcFilename;
	public String etag;
}

public class LiveLibCOSSync {
	
	public static String RootDir = "LiveLib/";
	public static String PC_ROOT_DIR = "G:/_WS/ADEV/eclipse-workspace/LiveLibCOSSync/LiveLib/";
	public static String bucketName = "g4-livelib-cos-1257773597";
	public static int MAX_UPLOAD_COUNT = 100;

    public static void main(String[] args) throws Exception {

    	try {
    		
	        COSCredentials cred = new BasicCOSCredentials("AKIDOsBKI5N5IJBvTtMEEBxa3OceXFtPR03u", "1sLhw20oTityx8D7pTwEbv5683nMl9MZ");
	        ClientConfig clientConfig = new ClientConfig(new Region("ap-chengdu"));
	        COSClient cosclient = new COSClient(cred, clientConfig);
	
	    	SyncUtilsPC suPC = new SyncUtilsPC();
	    	SyncUtilsCOS suCOS = new SyncUtilsCOS();
	    	
	    	List<tObjectInfo> PCObjList = suPC.getAllPCObjectList(PC_ROOT_DIR);
	    	
	    	List<tObjectInfo> COSObjList = suCOS.getAllCOSObjectList(cosclient, bucketName);
	    	
	    	COSObjList = suCOS.COSDelEmptyFolder(COSObjList, cosclient, bucketName);
	    	
	    	COSObjList = suCOS.COSDelExpireObj(COSObjList, PCObjList, cosclient, bucketName);
	    	
	    	suCOS.COSUploadPCObj(COSObjList, PCObjList, cosclient, bucketName);
	    	
	    	cosclient.shutdown();
	    	
	    	System.out.printf( "LiveLibCOSSync done" );
    	
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
    	
    }

}
