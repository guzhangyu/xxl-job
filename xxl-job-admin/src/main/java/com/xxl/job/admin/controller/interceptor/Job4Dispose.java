package com.xxl.job.admin.controller.interceptor;

import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.service.impl.JobUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Component
public class Job4Dispose implements InitializingBean,DisposableBean {

    @Resource
    XxlJobLogDao xxlJobLogDao;


    @Override
    public void destroy() throws Exception {
        updateLeftChildSummarys();
    }


    /**
     * 在停机前清理剩余的日志的childSummarys
     */
    public void updateLeftChildSummarys(){
        for(Map.Entry<Integer, List<Integer>> entry : JobUtils.parentIdChildMap.entrySet()){
            Integer parentId=entry.getKey();
            List<Integer> list=entry.getValue();
            synchronized (list){
                int left=list.size();
                if(left==0){
                    continue;
                }

                XxlJobLog log=xxlJobLogDao.load(parentId);
                List<XxlJobLog> logs=xxlJobLogDao.pageList(0,100,log.getJobGroup(),0,null,null,-2,parentId);

                int callSuccess=0;
                int callFails=0;
                int ignores=0;
                int triggerFails=0;
                for(XxlJobLog l:logs){
                    if(l.getTriggerCode()==200){
                        if(l.getHandleCode()!=200){
                            callFails++;
                        }else{
                            callSuccess++;
                        }
                    }else if(l.getTriggerCode()==600){
                        ignores++;
                    }else{
                        triggerFails++;
                    }
                    if(list.contains(l.getJobId())){
                        left--;
                    }
                }

                XxlJobLog toUpdate=new XxlJobLog();
                toUpdate.setId(parentId);
                toUpdate.setChildSummary(String.format("停机丢弃:%d,跳过:%d,调度失败:%d,执行失败:%d,成功:%d",left,ignores,triggerFails,callFails,callSuccess));
                xxlJobLogDao.updateChildSummary(toUpdate);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
