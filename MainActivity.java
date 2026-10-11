package com.example.yellowqa;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ImageView imgDisplay;
    private TextView txtStatus;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        imgDisplay = findViewById(R.id.imgDisplay);
        txtStatus = findViewById(R.id.txtStatus);
        searchInput = findViewById(R.id.searchInput);
        Button btnUpload = findViewById(R.id.btnUpload);

        // فتح معرض الصور لاختيار الـ 300+ صورة
        ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    processAndSaveImages(uris);
                }
            }
        );

        btnUpload.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void processAndSaveImages(List<Uri> uris) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;

        for (Uri uri : uris) {
            String path = uri.toString();
            // استخراج النص (عبر Tesseract OCR)
            String extractedText = "نص السؤال المستخرج من الصورة"; 

            db.execSQL("INSERT OR REPLACE INTO qa_data (image_path, extracted_text) VALUES (?, ?)",
                    new Object[]{path, extractedText});
            count++;
        }

        txtStatus.setText("✅ تم حفظ " + count + " صورة في قاعدة البيانات بنجاح!");
    }

    public void searchDatabase(String query) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT image_path FROM qa_data WHERE extracted_text LIKE ?", 
                new String[]{"%" + query + "%"});

        if (cursor.moveToFirst()) {
            String imagePath = cursor.getString(0);
            imgDisplay.setImageURI(Uri.parse(imagePath));
            txtStatus.setText("✅ تم العثور على الإجابة!");
        } else {
            txtStatus.setText("❌ لم يتم العثور على مطابقة.");
        }
        cursor.close();
    }
}

// كلاس إدارة قاعدة البيانات SQLite
class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "YellowQA.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS qa_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "image_path TEXT UNIQUE, " +
                "extracted_text TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS qa_data");
        onCreate(db);
    }
}