clear_s3_volumes:
	rm -rf ./s3/minio-1/* ./s3/minio-1/.[!.]*
	rm -rf ./s3/minio-2/* ./s3/minio-2/.[!.]*
	rm -rf ./s3/minio-3/* ./s3/minio-3/.[!.]*
	rm -rf ./s3/minio-4/* ./s3/minio-4/.[!.]*
