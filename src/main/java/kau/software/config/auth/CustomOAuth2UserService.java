package kau.software.config.auth;

import kau.software.config.auth.dto.OAuthAttributes;
import kau.software.domain.user.Role;
import kau.software.domain.user.UserRepository;
import kau.software.domain.user.Users;
import kau.software.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        Map<String, Object> newAttributes = makeNewAttributes(oAuth2User, registrationId);


//        Users user = saveOrUpdate(attributes);

        return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority(Role.USER.toString())),
                newAttributes,
                attributes.getNameAttributeKey());
    }
    private Map<String, Object> makeNewAttributes(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> newAttributes = new HashMap<>();
        Map<String, Object> oAuthAttributes = oAuth2User.getAttributes();

        if(registrationId.equals("naver")) {
            Map<String, Object> response = (Map<String, Object>)oAuthAttributes.get("response");
            for (String s : response.keySet()) {
                newAttributes.put(s, response.get(s));
            }
        }
        else{
            for (String s : oAuthAttributes.keySet()) {
                newAttributes.put(s, oAuthAttributes.get(s));
            }
        }
        newAttributes.put("oauth", "oauth");
        return newAttributes;
    }
    private Users saveOrUpdate(OAuthAttributes attributes) {
        Users user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getName()))
                .orElse(attributes.toEntity());

        return user;
    }
}
