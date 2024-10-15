package com.macro.mall.portal.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.macro.mall.common.constant.AuthConstant;
import com.macro.mall.common.dto.UserDto;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.common.util.RegexUtils;
import com.macro.mall.mapper.UmsMemberLevelMapper;
import com.macro.mall.mapper.UmsMemberMapper;
import com.macro.mall.model.UmsMember;
import com.macro.mall.model.UmsMemberExample;
import com.macro.mall.model.UmsMemberLevel;
import com.macro.mall.model.UmsMemberLevelExample;
import com.macro.mall.portal.service.UmsMemberCacheService;
import com.macro.mall.portal.service.UmsMemberService;
import com.macro.mall.portal.util.StpMemberUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 会员管理Service实现类
 * Created by macro on 2018/8/3.
 */
@Service
public class UmsMemberServiceImpl implements UmsMemberService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsMemberServiceImpl.class);
    @Autowired
    private UmsMemberMapper memberMapper;
    @Autowired
    private UmsMemberLevelMapper memberLevelMapper;
    @Autowired
    private UmsMemberCacheService memberCacheService;
    @Resource
    private RedissonClient redissonClient;

    @Override
    public UmsMember getByUsername(String username) {
        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andUsernameEqualTo(username);
        List<UmsMember> memberList = memberMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(memberList)) {
            return memberList.get(0);
        }
        return null;
    }

    @Override
    public UmsMember getById(Long id) {
        return memberMapper.selectByPrimaryKey(id);
    }

    @Override
    public void register(String username, String password, String telephone, String authCode) {
        if (RegexUtils.invalidUsername(username)) {
            Asserts.fail("用户名不符合要求格式");
        }

        if (RegexUtils.invalidPassword(password)) {
            Asserts.fail("密码不符合要求格式");
        }

        if (RegexUtils.invalidPhone(telephone)) {
            Asserts.fail("手机号码不符合要求格式");
        }

        // 验证验证码
        if(invalidAuthCode(authCode, telephone)){
            Asserts.fail("验证码错误");
        }

        // 验证通过，删除验证码缓存
        memberCacheService.delAuthCode(telephone);

        //查询是否已有该用户
        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andUsernameEqualTo(username);
        example.or(example.createCriteria().andPhoneEqualTo(telephone));
        RLock usernameLock = redissonClient.getLock("lock:ums:register:" + username);
        RLock telLock = redissonClient.getLock("lock:ums:register:" + telephone);
        if(!usernameLock.tryLock() || !telLock.tryLock()) {
            Asserts.fail("该用户已经存在");
        }
        try {
            List<UmsMember> umsMembers = memberMapper.selectByExample(example);
            if (!CollectionUtils.isEmpty(umsMembers)) {
                Asserts.fail("该用户已经存在");
            }

            // 没有该用户进行添加操作
            UmsMember umsMember = new UmsMember();
            umsMember.setUsername(username);
            umsMember.setPhone(telephone);
            umsMember.setPassword(BCrypt.hashpw(password));
            umsMember.setCreateTime(new Date());
            umsMember.setStatus(1);

            // 获取默认会员等级并设置
            UmsMemberLevelExample levelExample = new UmsMemberLevelExample();
            levelExample.createCriteria().andDefaultStatusEqualTo(1);
            List<UmsMemberLevel> memberLevelList = memberLevelMapper.selectByExample(levelExample);
            if (!CollectionUtils.isEmpty(memberLevelList)) {
                umsMember.setMemberLevelId(memberLevelList.get(0).getId());
            }
            memberMapper.insert(umsMember);
        } finally {
            usernameLock.unlock();
            telLock.unlock();
        }
    }

    @Override
    public String generateAuthCode(String telephone) {
        String authCode = memberCacheService.getAuthCode(telephone);
        if (authCode != null) {
            Asserts.fail("验证码已发送，请稍后再试");
        }
        RLock rLock = redissonClient.getLock("lock:ums:authCode:" + telephone);
        if(!rLock.tryLock()) {
            Asserts.fail("验证码已发送，请稍后再试");
        }
        try {
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            for(int i = 0; i < 6; i++){
                sb.append(random.nextInt(10));
            }
            memberCacheService.setAuthCode(telephone, sb.toString());
            return sb.toString();
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public void updatePassword(String telephone, String password, String authCode) {

        if (RegexUtils.invalidPassword(password)) {
            Asserts.fail("密码不符合要求格式");
        }

        if (RegexUtils.invalidPhone(telephone)) {
            Asserts.fail("手机号码不符合要求格式");
        }

        if(invalidAuthCode(authCode, telephone)){
            Asserts.fail("验证码错误");
        }

        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andPhoneEqualTo(telephone);
        List<UmsMember> memberList = memberMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(memberList)){
            Asserts.fail("该账号不存在");
        }
        UmsMember umsMember = memberList.get(0);
        umsMember.setPassword(BCrypt.hashpw(password));
        memberMapper.updateByPrimaryKeySelective(umsMember);
        memberCacheService.delMember(umsMember.getId());

        // TODO 后台标记其当前会话为“待验证”，当用户尝试执行敏感操作（如转账、修改个人信息等）时，系统会提示用户进行验证
        // 下线用户，强制用户重新登录
        logout();
    }

    @Override
    public UmsMember getCurrentMember() {
        UserDto userDto = (UserDto) StpMemberUtil.getSession().get(AuthConstant.STP_MEMBER_INFO);
        UmsMember member = memberCacheService.getMember(userDto.getId());
        if (member == null) {
            member = getById(userDto.getId());
            memberCacheService.setMember(member);
        }
        return member;
    }

    @Override
    public void updateIntegration(Long id, Integer integration) {
        UmsMember record=new UmsMember();
        record.setId(id);
        record.setIntegration(integration);
        memberMapper.updateByPrimaryKeySelective(record);
        memberCacheService.delMember(id);
    }

    @Override
    public SaTokenInfo login(String username, String password) {

        if (RegexUtils.invalidUsername(username)) {
            Asserts.fail("用户名不符合要求格式");
        }

        if (RegexUtils.invalidPassword(password)) {
            Asserts.fail("密码不符合要求格式");
        }

        UmsMember member = getByUsername(username);
        if(member == null) {
            Asserts.fail("找不到该用户！");
        }
        if (!BCrypt.checkpw(password, member.getPassword())) {
            Asserts.fail("密码不正确！");
        }
        if(member.getStatus() != 1){
            Asserts.fail("该账号已被禁用！");
        }

        // 登录校验成功后，一行代码实现登录
        StpMemberUtil.login(member.getId());
        UserDto userDto = new UserDto();
        userDto.setId(member.getId());
        userDto.setUsername(member.getUsername());
        userDto.setClientId(AuthConstant.PORTAL_CLIENT_ID);
        // 将用户信息存储到Session中
        StpMemberUtil.getSession().set(AuthConstant.STP_MEMBER_INFO, userDto);
        // 获取当前登录用户Token信息
        return StpUtil.getTokenInfo();
    }

    @Override
    public void logout() {
        //先清空缓存
        UserDto userDto = (UserDto) StpMemberUtil.getSession().get(AuthConstant.STP_MEMBER_INFO);
        memberCacheService.delMember(userDto.getId());
        //再调用sa-token的登出方法
        StpMemberUtil.logout();
    }

    // 对输入的验证码进行校验
    private boolean invalidAuthCode(String authCode, String telephone){
        if (StrUtil.isEmpty(authCode)){
            return true;
        }
        String realAuthCode = memberCacheService.getAuthCode(telephone);
        if (realAuthCode == null) {
            Asserts.fail("验证码已过期");
        }
        return !authCode.equals(realAuthCode);
    }

}
