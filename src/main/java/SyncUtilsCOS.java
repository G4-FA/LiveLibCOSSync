import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;

class SyncUtilsCOS {

	
	// 鑾峰彇COS涓婃墍鏈夊璞＄殑鍒楄〃
    public List<tObjectInfo> getAllCOSObjectList( COSClient cosclient, String bucketName) {
    	
    	System.out.println( "[SyncUtilsCOS getAllCOSObjectList]" );
    	
    	List<tObjectInfo> retList = new ArrayList<tObjectInfo>();

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.setBucketName(bucketName);
        listObjectsRequest.setPrefix(LiveLibCOSSync.RootDir);
        listObjectsRequest.setDelimiter("");
        listObjectsRequest.setMaxKeys(1000); //鍗曟澶у皬 涓嶇敤鎷呭績涓嶈冻 getNextMarker浼氱户缁線涓嬮亶鍘�
        ObjectListing objectListing = null;

        do {

            try {
                objectListing = cosclient.listObjects(listObjectsRequest);
            } catch (CosServiceException e) {
                e.printStackTrace();
                return null;
            } catch (CosClientException e) {
                e.printStackTrace();
                return null;
            }

            List<COSObjectSummary> cosObjectSummaries = objectListing.getObjectSummaries();
            for (COSObjectSummary cosObjectSummary : cosObjectSummaries) {
            	
                String key = cosObjectSummary.getKey();
                String etag = cosObjectSummary.getETag();
                long fileSize = cosObjectSummary.getSize();
                String storageClasses = cosObjectSummary.getStorageClass();
                
                if ( key.equals(LiveLibCOSSync.RootDir) ) { continue; }
                
                tObjectInfo info = new tObjectInfo(key, fileSize, "COSFilename", etag);
                retList.add(info);

                System.out.printf( "Find COS Object --- key:%s etag:%s filesize:%d sc:%s\n", key,etag,fileSize,storageClasses );
            }
            

            String nextMarker = objectListing.getNextMarker();
            listObjectsRequest.setMarker(nextMarker);
        } while (objectListing.isTruncated());
    	
        return retList;
    	
    }
	
	
	// 鍒犻櫎COS涓婄殑绌烘枃浠跺す 骞惰繑鍥炲墧闄ょ┖鏂囦欢澶瑰悗鐨勫璞″垪琛�
	// 鏈韩COS娌℃湁鏂囦欢澶� 鏄鑰佷範鎯殑妯℃嫙 瀹為檯鏄┖瀵硅薄 浣嗘槸绠＄悊璧锋潵涓嶇ǔ瀹�
    public List<tObjectInfo> COSDelEmptyFolder( List<tObjectInfo> COSObjList, COSClient cosclient, String bucketName ) {
    	
    	System.out.println( "[SyncUtilsCOS COSDelEmptyFolder]" );
    	
    	List<tObjectInfo> retList = new ArrayList<tObjectInfo>();

        // 鍒犻櫎绌烘枃浠跺す
		for( int i=0; i < COSObjList.size(); i++) {
			
        	int needDel = 0;
			tObjectInfo info = COSObjList.get(i);
            if ( info.size <= 0 && !info.key.equals(LiveLibCOSSync.RootDir) && info.key.endsWith("/") ) {
            	
            	needDel = 1;
            	
            	for( int j=0; j<COSObjList.size(); j++) {
            		tObjectInfo info2 = COSObjList.get(j);
            		if ( info2.key.contains( info.key ) && info2.key.length() > info.key.length() ) {
            			needDel = 0;
            			break;
            		}
            	}

            }
            
            if ( needDel == 1 ) {
				try {
	            	cosclient.deleteObject(bucketName, info.key);
					System.out.printf( "Del Empty Folder --- key:%s\n", info.key );
	            } catch (CosServiceException e) {
	                e.printStackTrace();
	                return null;
	            } catch (CosClientException e) {
	                e.printStackTrace();
	                return null;
	            }
            } else {
    			retList.add( info );
            }

		}
		
		return retList;

    }

    
	// 鍒犻櫎COS涓婄殑搴熷純瀵硅薄
    // 鐩墠鍒ゅ畾鏂规硶鏄疨C搴撲腑涓嶅瓨鍦╫r 澶у皬鍜孭C搴撲笉鍚�
    // 杩斿洖鍊间负鍓╀綑鐨凜OS瀵硅薄鍒楄〃
    public List<tObjectInfo> COSDelExpireObj( List<tObjectInfo> COSObjList, List<tObjectInfo> PCObjList, COSClient cosclient, String bucketName ) {
    	
    	System.out.println( "[SyncUtilsCOS COSDelExpireObj]" );
    	
    	List<tObjectInfo> retList = new ArrayList<tObjectInfo>();
    	
		for( int i = 0 ; i < COSObjList.size() ; i++) {
			
			tObjectInfo infoCOS = COSObjList.get(i);
			
			int isFind = 0;
			int isKeep = 0;
			for( int j=0; j<PCObjList.size(); j++) {
				
				tObjectInfo infoPC = PCObjList.get(j);
				if ( infoCOS.key.equals(infoPC.key) ) {
					isFind = 1;
					if ( infoCOS.size == infoPC.size ) {
						isKeep = 1;
					} else {
						isKeep = 0;
					}
				}

			}
			
			if ( isFind == 1 && isKeep == 1 ) {
				retList.add( infoCOS );
			} else {
				
    			try {
	            	cosclient.deleteObject(bucketName, infoCOS.key);
	            } catch (CosServiceException e) {
	                e.printStackTrace();
	                return null;
	            } catch (CosClientException e) {
	                e.printStackTrace();
	                return null;
	            }
    			
    			System.out.printf( "Del Expire Obj --- key:%s find:%d keep:%d\n", infoCOS.key, isFind, isKeep );
    			
			}

		}
    	
		
		return retList;
    }
    
    

	// 灏哖C瀛樺湪COS涓嶅瓨鍦ㄧ殑瀵硅薄涓婁紶鍒癈OS
    public void COSUploadPCObj( List<tObjectInfo> COSObjList, List<tObjectInfo> PCObjList, COSClient cosclient, String bucketName ) {
    	
    	System.out.println( "[SyncUtilsCOS COSUploadPCObj]" );
    	
    	int uploadCount = 0;
    	
		for( int i=0; i<PCObjList.size(); i++) {
			
			tObjectInfo infoPC = PCObjList.get(i);
			
			int isFind = 0;
			for( int j=0; j<COSObjList.size(); j++) {
				if ( COSObjList.get(j).key.equals(infoPC.key) ) { isFind = 1; }
			}
			
			if ( isFind == 0 ) {
				
    			try {
    				
    				File localFile = new File(infoPC.pcFilename);
    				PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, infoPC.key, localFile);
    				PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
    				
        			System.out.printf( "Upload PC Obj --- key:%s size:%d etag:%s\n", infoPC.key, infoPC.size, putObjectResult.getETag() );
        			
        			uploadCount++;
        			if ( uploadCount >= LiveLibCOSSync.MAX_UPLOAD_COUNT ) {
        				System.out.printf( "Upload PC Obj --- WARNING MAX_UPLOAD_COUNT\n" );
        				return;
        			}
    				
	            } catch (CosServiceException e) {
	                e.printStackTrace();
	                return;
	            } catch (CosClientException e) {
	                e.printStackTrace();
	                return;
	            }
				
			}

		}
    	
    	
    }
	
}
