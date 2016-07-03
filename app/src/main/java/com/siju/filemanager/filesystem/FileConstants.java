package com.siju.filemanager.filesystem;

/**
 * Created by Siju on 13-06-2016.
 */

public class FileConstants {

    public static final String KEY_PATH = "PATH";
    public static final String KEY_FILENAME = "FILENAME";
    public static final String KEY_DUAL_MODE = "DUAL_MODE";
    public static final String APK_EXTENSION = "apk";
    public static final String KEY_CATEGORY = "CATEGORY";
    public static final String KEY_ZIP = "ZIP";


    public static final int KEY_SORT_NAME = 0;
    public static final int KEY_SORT_NAME_DESC = 1;

    public static final int KEY_SORT_TYPE = 2;
    public static final int KEY_SORT_TYPE_DESC = 3;

    public static final int KEY_SORT_SIZE = 4;
    public static final int KEY_SORT_SIZE_DESC = 5;

    public static final int KEY_SORT_DATE = 6;
    public static final int KEY_SORT_DATE_DESC = 7;


    public static final int KEY_LISTVIEW = 0;
    public static final int KEY_GRIDVIEW = 1;






    /********** DOCUMENT EXTENSIONS**************/
    public static final String EXT_TEXT = "txt";
    public static final String EXT_HTML = "html";
    public static final String EXT_PDF = "pdf";
    public static final String EXT_DOC = "doc";
    public static final String EXT_DOCX = "docx";
    public static final String EXT_XLS = "xls";
    public static final String EXT_XLXS = "xlxs";
    public static final String EXT_PPT = "ppt";
    public static final String EXT_PPTX = "pptx";

    public enum CATEGORY {
        FILES(0),
        AUDIO(1),
        VIDEO(2),
        IMAGE(3),
        DOCS(4);

        private int value;

        private CATEGORY(int value) {
            this.value = value;
        }

        public int getValue() {

            return value;
        }
    }

    public enum DOC_TYPES {
        PDF(0),
        TXT(1),
        DOC(2),
        PPT(3),
        XLS(4),
        XLXS(5);
        private int value;

        private DOC_TYPES(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }


}
