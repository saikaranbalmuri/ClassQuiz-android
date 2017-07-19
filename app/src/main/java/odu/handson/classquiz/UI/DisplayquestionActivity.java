package odu.handson.classquiz.UI;

import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import odu.handson.classquiz.R;

import odu.handson.classquiz.adapter.QuestionListAdapter;
import odu.handson.classquiz.model.ActiveQuestionDetails;
import odu.handson.classquiz.model.Answers;

import odu.handson.classquiz.utility.ServiceCaller;

import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;


import java.io.IOException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import odu.handson.classquiz.model.ActiveQuestionDetails.DataActive.Question;

public class DisplayquestionActivity extends AppCompatActivity {
    public static List<Question> question_details;//=new ArrayList<Question>();
    public String userId;
    public int quizid, sizeQuiz,studentQuizId;
    public static String presentQuizId;
    Button quizSubmitButton;
    static SharedPreferences sharedPreferences;
    public static Map quesOptionMap;
    long sMillisUntilFinished;
    CountDownTimer countDownTimer;
    int timeUp=0;
    String response;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_displayquestion);
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        presentQuizId=getIntent().getStringExtra("quizId");
        userId = getIntent().getStringExtra("userId");
        if(savedInstanceState==null) {
            quesOptionMap = new HashMap<String, String>();
            quesOptionMap.put("quizid", getIntent().getStringExtra("quizId"));
        }
        if(sharedPreferences.getString("quesOptionMap"+presentQuizId,null)!=null) {
            HashMap myMap = new HashMap<String,String>();
            System.out.println("in recover map is-----" + sharedPreferences.getString("quesOptionMap"+presentQuizId, null));
           String mapString=sharedPreferences.getString("quesOptionMap"+presentQuizId, null);
            String value = mapString.substring(mapString.indexOf("{") + 1, mapString.indexOf("}"));
            String[] pairs = value.split(",");
            for (int i=0;i<pairs.length;i++) {
                String pair = pairs[i];
                String[] keyValue = pair.split("=");
                System.out.println("question map is---"+keyValue[0].trim()+"--"+keyValue[1].trim());
                myMap.put(keyValue[0].trim(), keyValue[1].trim());
            }

            if(presentQuizId.equals(myMap.get("quizid"))){
               // quesOptionMap=myMap;
                for (int i=0;i<pairs.length;i++) {
                    String pair = pairs[i];
                    String[] keyValue = pair.split("=");
                    //System.out.println("question map is---"+keyValue[0].trim()+"--"+keyValue[1].trim());
                    //quesOptionMap.put(keyValue[0].trim(), keyValue[1].trim());
                    String key=keyValue[0].trim(),val=keyValue[1].trim();
                    quesOptionMap.put(key,val);
                }
//            System.out.println("question map is---"+quesOptionMap.toString());
            }

        }

        quizSubmitButton = (Button) findViewById(R.id.submitQuiz);
        quizSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendQuizData();
            }
        });


        if(isNetworkAvailable())
            new RequestQuestionsService().execute(getIntent().getStringExtra("userId"), getIntent().getStringExtra("quizId"));
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Network Unavailable");
            builder.setMessage("Your device is currently unable to establish a network connection.\nTurn on cellular data or use Wi-Fi to access data.");
            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent in=new Intent(DisplayquestionActivity.this,LoginActivity.class);
                    startActivity(in);
                }
            });

            builder.show();
        }
    }
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onBackPressed() {
    }
  /*  @Override
    protected void onResume() {
        super.onResume();
        flag=1;
        new RequestQuestionsService().execute(getIntent().getStringExtra("userId"), getIntent().getStringExtra("quizId"));
    }*/

    /*@Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Time", Long.toString(sMillisUntilFinished));
        System.out.println("stored to pref " + Long.toString(sMillisUntilFinished));
        editor.commit();
        countDownTimer.cancel();
    }
*/
    class RequestQuestionsService extends AsyncTask<String, Integer, String> {
        public ProgressDialog dialog = new ProgressDialog(DisplayquestionActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("loading...");
            dialog.show();
        }


        @Override
        protected String doInBackground(String... params) {
            return ServiceCaller.startQuestionPostService("https://qav2.cs.odu.edu/mobile/start_quiz.php", "userId", params[0], "quizId", params[1]);
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println("Result is " + result);
            ObjectMapper mapper = new ObjectMapper();
            try {
                ActiveQuestionDetails aqd = mapper.readValue(result, ActiveQuestionDetails.class);
                System.out.println("student quiz id "+aqd.getData().getStudentQuizId());
                studentQuizId=aqd.getData().getStudentQuizId();
                final TextView timer = (TextView) findViewById(R.id.timer);
                countDownTimer = new CountDownTimer(aqd.getData().getDuration()*1000, 1000) {

                    @Override
                    public void onTick(long millisUntilFinished) {
                        timer.setText(String.format("%02d", (millisUntilFinished / (1000 * 60) )) + ":" + String.format("%02d", (millisUntilFinished / 1000) % 60));
                        sMillisUntilFinished = millisUntilFinished;
                    }

                    @Override
                    public void onFinish() {
                        System.out.println("finished");
                        AlertDialog.Builder alert = new AlertDialog.Builder(DisplayquestionActivity.this);
                        alert.setTitle("Alert");
                        alert.setMessage("Time Up!");
                        alert.setCancelable(false);
                        alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                timeUp=1;
                                sendQuizData();
                                Intent in = new Intent(getApplicationContext(), DisplayquizActivity.class);
                                in.putExtra("userId", userId);
                                startActivity(in);
                                dialog.cancel();
                            }
                        });
//                        SharedPreferences.Editor editor = sharedPreferences.edit();
//                        editor.putString("Time", "0");
//                        editor.commit();
                        AlertDialog alertDialog = alert.create();
                        alertDialog.show();
                    }
                }.start();

                if (aqd.getStatusCode() == 200) {
                    question_details = aqd.getData().getQuestions();
                    sizeQuiz = aqd.getData().getQuestions().size();
                    System.out.println(question_details + "--size--" + aqd.getData().getQuestions().size());


                    QuestionListAdapter QuestionListAdapter = new QuestionListAdapter(getApplicationContext(), question_details);
                    ListView listView = (ListView) findViewById(R.id.questionList);
                    listView.setAdapter(QuestionListAdapter);
                } else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(DisplayquestionActivity.this);
                    alert.setTitle("Alert");
                    alert.setMessage("Error in starting quiz!");
                    alert.setCancelable(false);
                    alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent in = new Intent(getApplicationContext(), DisplayquizActivity.class);
                            in.putExtra("userId", userId);
                            startActivity(in);
                            dialog.cancel();
                        }
                    });
                    AlertDialog alertDialog = alert.create();
                    alertDialog.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            dialog.cancel();
        }
    }

    public static void mapQuesAns(int qid, int optionid) {
        quesOptionMap.put(qid+"", optionid+"");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("quesOptionMap"+presentQuizId,quesOptionMap.toString());
        editor.commit();
        System.out.println("map is ----- "+sharedPreferences.getString("quesOptionMap"+presentQuizId,null));

    }

    public void sendQuizData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Time", "0");
        editor.commit();

        final List<Answers.UserAnswers> answers = new ArrayList<Answers.UserAnswers>();
        final Answers ans = new Answers();

        for (Object key : quesOptionMap.keySet()) {
            if(!key.equals("quizid")) {
                Log.i("Map", "qid-" + key + "  option id-" + quesOptionMap.get(key));
//            QuizResponse qr=new QuizResponse();
//            qr.setOptionId(Integer.parseInt(quesOptionMap.get(key).toString()));
//            qr.setQuestionId(Integer.parseInt(key.toString()));
                Answers.UserAnswers ua = new Answers.UserAnswers();
                ua.setQuestionId(Integer.parseInt(key.toString()));
                ua.setOptionId(Integer.parseInt(quesOptionMap.get(key).toString()));
                answers.add(ua);
            }
        }
        ans.setAnswers(answers);
        ans.setStudentQuizId(quizid);
        ObjectMapper mapper = new ObjectMapper();

        try {
            response = mapper.writeValueAsString(answers);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(timeUp==1)
            new SendAnswers().execute(Integer.toString(studentQuizId), response);
        else {
            System.out.println("before alert");

            AlertDialog.Builder alert = new AlertDialog.Builder(DisplayquestionActivity.this);
            alert.setTitle("Message");
            alert.setMessage("You answered " + (quesOptionMap.size()-1)+ " out of " + sizeQuiz + " questions.");
            alert.setCancelable(false);
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    // finish();
                }
            });
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {
                        System.out.println(response);
                        System.out.println(quesOptionMap.size());
                        if (quesOptionMap.size() == 1) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(DisplayquestionActivity.this);
                            alert.setTitle("Message");
                            alert.setMessage("Answer atleast one question.");
                            alert.setCancelable(false);
                            alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            AlertDialog alertDialog = alert.create();
                            alertDialog.show();
                        } else if ((quesOptionMap.size()-1) != sizeQuiz) {
                            AlertDialog.Builder inneralert = new AlertDialog.Builder(DisplayquestionActivity.this);
                            inneralert.setTitle("Message");
                            inneralert.setMessage("You haven't attempted all questions.\nDo you wish to continue?");
                            inneralert.setCancelable(false);
                            inneralert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            inneralert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    System.out.println("inside yes");
                                    new SendAnswers().execute(Integer.toString(studentQuizId), response);
                                    dialog.cancel();
                                }
                            });

                            AlertDialog inneralertDialog = inneralert.create();
                            inneralertDialog.show();
                        } else
                            new SendAnswers().execute(Integer.toString(studentQuizId), response);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    dialog.cancel();
                }
            });
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        }

    }

    class SendAnswers extends AsyncTask<String, Integer, String> {
        public ProgressDialog dialog = new ProgressDialog(DisplayquestionActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("sending answers...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            return ServiceCaller.startPostService("https://qav2.cs.odu.edu/mobile/submit_quiz_answers.php", "studentQuizId", params[0], "answers", params[1]);
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println("submit quiz response " + result);
            try {
                JSONObject json = new JSONObject(result);
                System.out.println(json.get("statusCode"));
                if (json.get("statusCode").equals(200)) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("quesOptionMap"+presentQuizId,null);
                    editor.commit();
                    System.out.println("in if");
                    AlertDialog.Builder alert = new AlertDialog.Builder(DisplayquestionActivity.this);
                    alert.setTitle("Message");
                    alert.setMessage("Submitted answers sucessfully");
                    alert.setCancelable(false);
                    alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent in = new Intent(getApplicationContext(), DisplayquizActivity.class);
                            in.putExtra("userId", userId);
                            startActivity(in);
                            dialog.cancel();
                        }
                    });
                    AlertDialog alertDialog = alert.create();
                    alertDialog.show();

                } else {
                    System.out.println("in else");
                    AlertDialog.Builder alert = new AlertDialog.Builder(DisplayquestionActivity.this);
                    alert.setTitle("Alert");
                    alert.setMessage(json.getString("msg"));
                    alert.setCancelable(false);
                    alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent in = new Intent(getApplicationContext(), DisplayquizActivity.class);
                            in.putExtra("userId", userId);
                            startActivity(in);
                            dialog.cancel();
                        }
                    });
                    AlertDialog alertDialog = alert.create();
                    alertDialog.show();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            dialog.cancel();
        }

    }
}