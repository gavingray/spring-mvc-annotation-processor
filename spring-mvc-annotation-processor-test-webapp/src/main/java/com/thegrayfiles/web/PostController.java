package com.thegrayfiles.web;

import com.thegrayfiles.marshallable.TestEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PostController {

    @RequestMapping(value="/canPostWithoutParametersOrReturnValue", method = RequestMethod.POST)
    public @ResponseBody void canPostWithoutParametersOrReturnValue() {

    }

    @RequestMapping(value="/canPostWithRequestBody", method = RequestMethod.POST)
    public @ResponseBody TestEntity canPostWithRequestBody(@RequestBody TestEntity testEntity) {
        return new TestEntity(testEntity.getName());
    }

    @RequestMapping(value="/canPostWithRequestBodyAndRequestParam", method = RequestMethod.POST)
    public @ResponseBody TestEntity canPostWithRequestBodyAndRequestParam(@RequestBody TestEntity testEntity, @RequestParam String requestParam) {
        TestEntity returnValue = new TestEntity(testEntity.getName());
        returnValue.addRequestParameterValue(requestParam);
        return returnValue;
    }
}

