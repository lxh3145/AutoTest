package com.token;

import com.alibaba.fastjson.JSON;
import com.qa.base.TestBase;
import com.qa.Parameters.postParameters;
import com.qa.restclient.RestClient;
import com.qa.util.TestUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.*;
import java.io.IOException;
import java.util.HashMap;
import static com.qa.util.TestUtil.dtt;

public class EFPStagingCN extends TestBase {

    TestBase testBase;
    RestClient restClient;
    CloseableHttpResponse closeableHttpResponse;
    //host
    String host;
    //Excel路径
    String testCaseExcel;
    //token路径
    String tokenPath;
    //header
    HashMap<String ,String> postHeader = new HashMap<String, String>();
    //登录token
    HashMap<String,String> loginToken  = new HashMap<String, String>();
    @BeforeClass
    public void setUp(){
        testBase = new TestBase();
        postHeader.put("Content-Type","application/json");
        restClient = new RestClient();
        //接口endpoint
        host = prop.getProperty("Host");
        //读取配置文件Excel路径
        testCaseExcel=prop.getProperty("testCase1data");
        //读取配置文件token路径
        tokenPath = prop.getProperty("token");
    }

    @DataProvider(name = "postData")
    public Object[][] post() throws IOException {
        return dtt(testCaseExcel,0);

    }
    @DataProvider(name = "getData")
    public Object[][] get() throws IOException{
        return dtt(testCaseExcel,1);

    }

    @DataProvider(name = "deleteData")
    public Object[][] delete() throws IOException{
        return dtt(testCaseExcel,2);
    }



    @Test(dataProvider = "postData")
    public void login(String loginUrl,String username, String passWord) throws Exception {
        postParameters loginParameters = new postParameters(username,passWord);
        String userJsonString = JSON.toJSONString(loginParameters);
        //发送登录请求
        closeableHttpResponse = restClient.postApi(host+loginUrl,userJsonString,postHeader);
        //获取登录后的token
        loginToken = TestUtil.getToken(closeableHttpResponse,tokenPath);
        int statusCode = TestUtil.getStatusCode(closeableHttpResponse);
        Assert.assertEquals(statusCode,200);

    }

    @Test(dataProvider = "getData",dependsOnMethods = {"login"})
    public void getMothed(String url) throws Exception{
        //将token赋值后发送get请求
        closeableHttpResponse = restClient.getApi(host+url,loginToken);
        int statusCode = TestUtil.getStatusCode(closeableHttpResponse);
        Assert.assertEquals(statusCode,200);
    }

    @Test(dataProvider = "deleteData",dependsOnMethods = {"getMothed"})
    public void deleteMothed(String url) throws IOException{
        closeableHttpResponse = restClient.deleteApi(url);
        int statusCode = TestUtil.getStatusCode(closeableHttpResponse);
        Assert.assertEquals(statusCode,204);
    }

    //获取返回的token ,使用JsonPath获取json路径
    public static HashMap<String,String> getToken(CloseableHttpResponse closeableHttpResponse,String jsonPath) throws Exception{
        HashMap<String,String> responseToken = new HashMap<String, String>();
        String responseString = EntityUtils.toString( closeableHttpResponse.getEntity(),"UTF-8");
        ReadContext ctx = JsonPath.parse(responseString);
        String Token = ctx.read(jsonPath); //"$.EFPV3AuthenticationResult.Token"
        if(null == Token||"".equals(Token)){
            new Exception("token不存在");
        }
        responseToken.put("x-ba-token",Token);
        return responseToken;
    }
}