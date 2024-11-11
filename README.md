## 스프링 시큐리티(Spring Security)란?
>
**Spring Security**란 스프링 기반의 애플리케이션 보안(인증, 인가, 권한)을 담당하는 스프링 하위 프레임워크이다.

- 스프링 시큐리티를 이해하기 위해서는 ***인증(Authentication)***과 ***인가(Authorization)***에 대하여 알아야 한다.
***
### 인증(Authentication)과 인가(Authorization)

**인증(Authentication)**

인증(Authentication)은 사용자의 신원을 입증하는 과정

- 사용자가 사이트에 로그인을 할 때 누구인지 확인하는 과정이 인증의 대표적인 예이다.

**인가(Authorization)**

인가(Authorization)는 사이트의 특정 부분에 접근할 수 있는지 권한을 확인하는 작업

- 관리자는 관리자 페이지에 들어갈 수 있지만 일반 사용자는 관리자 페이지에 들어갈 수 없게 권한을 확인하는 과정이 인가의 대표적인 예이다.

이러한 스프링 시큐리티는 CSRF 공격(사용자의 권한을 가지고 특정 동작을 수행하도록 유도하는 공격), 세션 고정 공격(사용자의 인증 정보를 탈취하거나 변조하는 공격)을 방어해준다.

***

## 필터 기반으로 동작하는 스프링 시큐리티🧑🏻‍⚖️

- **스프링 시큐리티는 필터 기반으로 동작**한다.
- 다음 참고 사진은 스프링시큐리티의 필터 구조에 대해 나타낸 구조도이다! 유심히 관찰해보자🤓


![](https://velog.velcdn.com/images/kkong_do/post/d7e11e92-0cb8-4084-af92-4ee0116c5067/image.png)

- 스프링 시큐리티는 다양한 필터들로 이루어져 있으며, 각 필터에서 인증과 인가와 관련된 작업을 처리한다.
- **SecurityContextPersistenceFilter부터 시작해서 아래로 내려가며 FilterSecurityInterceptor까지 순서대로 필터를 거치게 된다.**
- 필터가 실행될 때마다 빨간색 화살표로 연결된 오른쪽 박스의 클래스를 거치며 실행이 된다.
- 특정 필터를 제거하거나 필터 뒤에 커스텀 필터를 넣는 등의 설정도 가능하다.

>
우리는 여기서 `UsernamePasswordAuthenticationFilter`와 `FilterSecurityInterceptor`를 집중해서 보아야 한다.

### UsernamePasswordAuthenticationFilter
>
아이디와 패스워드가 넘어오면 인증 요청을 위임하는 ***인증 관리자*** 역할을 수행한다.

- 폼 기반의 로그인을 수행할 때 사용되는 필터로 아이디, 패시워드 데이터를 파싱(Parsing)해 인증 요청을 위임한다.
- 인증이 성공하면 `AuthenticationSuccessHandler`를, 인증에 실패하면 `AuthenticationFailureHandler`를 실행한다.


### FilterSecurityInterceptor
>
권한 부여 처리를 위임하여 접근 제어 결정을 쉽게 하는 ***접근 결정 관리자*** 역할을 수행한다.

- AccessDecisionManager로 권한 부여 처리를 위임함으로써 접근 제어 결정을 쉽게 해준다.
- 이 과정에서는 이미 사용자가 인증되어 있으므로 유효한 사용자인지 아닌지 알 수 있다. 즉, 인가 관련 설정을 할 수 있다.
***

### 폼 로그인 기반 스프링 시큐리티 인증 처리 흐름
![](https://velog.velcdn.com/images/kkong_do/post/b9491848-3064-4bae-b037-1771cfe38887/image.png)

#### 다음 절차를 차근히 따라와 보자🦾

1. 사용자가 폼에 아이디와 패스워드를 입력하면, HTTPServletRequest에 아이디와 비밀번호 정보가 전달된다. 이때 AuthenticationFilter가 넘어온 아이디와 비밀번호의 유효성 검사를 진행한다.
2. 유효성 검사가 끝나면실제 구현체인 UsernamePasswordAuthenticationToken을 만들어 넘겨준다.
3. 전달받은 인증용 객체인 UsernamePasswordAuthenticationToken을 AuthenticationManager에게 보낸다.
4. UsernamePasswordAuthenticationToken을 AuthenticationProvider에 보낸다.
5. 사용자 아이디를 UserDetailService에 보낸다. UserDetailService는 사용자 아이디로 찾은 사용자의 정보를 UserDetails 객체로 만들어 AuthenticationProvider에게 전달한다.
6. DB에 있는사용자 정보를 가져온다.
7. 입력 정보와 UserDetails의 정보를 비교해 실제 인증 처리를 한다.
8. ~ 10.까지 인증이 완료되면 SecurityContextHolder에 Authentication을 저장한다. 인증 성공 여부에 따라 성공하면 AuthenticationSucessHandler, 실패하면 AuthenticationFailureHandler를 실행한다.

***
## Spring Security 구현 코드
- 이번 실습은 Form을 이용해서 로그인/로그아웃을 구현한 코드이다.


### 1. 의존성 추가하기(build.gradle)
```java
dependencies{
	// 스프링 시큐리티를 사용하기 위한 스타터 추가
	implementation 'org.springframework.boot:spring-boot-starter-security'
    // 타임리프에서 스프링 시큐리티를 사용하기 위한 의존성 추가
	implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
	// 스프링 시큐리티를 테시트하기 위한 의존성 추가
	testImplementation 'org.springframework.security:spring-security-test'
}

```
### 2. 엔티티 만들기
```java
package me.shinsunyoung.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "users")
@Builder
public class User implements UserDetails { // UserDetails를 상속받아 인증 객체로 사용
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "password")
    private String password;

    @Builder
    public User(Long id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }

    @Override // 권한 반환
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("user"));
    }

    // 사용자의 id를 반환(고유한 값)
    @Override
    public String getUsername() {
        return email;
    }

    // 사용자의 패스워드 반환
    @Override
    public String getPassword(){
        return password;
    }
    // 계정 만료 여부 반환
    @Override
    public boolean isAccountNonExpired() {
        return true; // true면 만료되지 않았음
    }

    // 계쩡 잠금 여부 반환
    @Override
    public boolean isAccountNonLocked() {
        return true; // true면 계정 잠금 X
    }

    // 패스워드의 만료 여부 반환
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 계정 사용 가능 여부 반환
    @Override
    public boolean isEnabled() {
        return true;
    }
}
```
- 유의해야할 점은 **UserDetails를 꼭 implements 받아서 구현**하여야 한다.

#### UserDetail 인터페이스에 선언된 메서드📒
|메서드|반환 타입|설명|
|-----|-----|-------|
|getAuthorities|Collection< ? extends GrantedAuthority>|사용자가 가지고 있는 권한의 목록을 반환한다.|
|getUsername()|String|사용자를 식별할 수 있는 사용자 이름을 반환한다. 이때 사용되는 사용자의 이름은 반드시 고유해야 한다.  현재 코드는 email이 고유 코드이다.|
|getPassword()|String|사용자의 비밀번호를 반환한다. 이때 저장되어 있는 비밀번호는 암호화해서 저장해야 한다.|
|isAccountNonExpired()|boolean|계정이 만료되었는지 확인하는 메서드이다. 만약 만료되지 않았따면 true를 반환한다.|
|isAccountNonLocked()|boolean|계정이 만료되었는지 확인하는 메서드이다. 만약 만료되지 않았따면 true를 반환한다.|
|isCredetialsNonExpired()|boolean|비밀번호가 만료되었는지 확인하는 메서드이다. 만약 만료되지 않았을 때는 true를 반환한다.|
|isEnabled()|boolean|계정이 사용 가능한지 확인하는 메서드이다. 만약 사요 가능하다면 true 반환하다.|

### 3. 리포지터리 만들기

- User 엔티티에 대한 리포지토리 만들기
```java
package me.shinsunyoung.springbootdeveloper.repository;

import me.shinsunyoung.springbootdeveloper.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```
### 4. 서비스 메서드 코드 작성하기
- 엔티티와 리포지터리가 완성되었으니 스프링 시큐리티에서 로그인을 진행할 때 사용자 정보를 가져오는 코드 작성
```java
package me.shinsunyoung.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.springbootdeveloper.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 스프링 시큐리티에서 사용자 정보를 가져오는 인터페이스
public class UserDetailService implements UserDetailsService {
    private final UserRepository userRepository;
    
    // 사용자 이름(email)으로 사요앚의 정보를 가져오는 메서드
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException(email));
    }
}

```
- 스프링 시큐리티에서 사용자의 정보를 가져오는 UserDetailsService 인터페이스를 구현한다.
- `loadUserByUsername()` 메서드를 오버라이딩해서 사용자 정보를 가져오는 로직 작성

### 5. 시큐리티 설정하기

- 실제 인증 처리를 하는 시큐리티 설정 파일 WebSecurityConfig파일을 작성한다.

```java
package me.shinsunyoung.springbootdeveloper.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final UserDetailsService userService;

    @Bean // 1. 스프링 시큐리티 기능 비활성화
    public WebSecurityCustomizer configure() {
        return (web) -> web.ignoring()
                .requestMatchers(new AntPathRequestMatcher("/static/**"));
    }

    @Bean // 2. 특정 HTTP 요청에 대한 웹 기반 보안 구성
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth // 3. 인증, 인가 설정
                        .requestMatchers(
                                new AntPathRequestMatcher("/login"),
                                new AntPathRequestMatcher("/signup"),
                                new AntPathRequestMatcher("/user")
                        ).permitAll()
                        .anyRequest().authenticated())
                .formLogin(formLogin -> formLogin // 4. 폼 기반 로그인 설정
                        .loginPage("/login")
                        .defaultSuccessUrl("/articles")
                )
                .logout(logout -> logout.logoutSuccessUrl("/login") // 5. 로그아웃 설정
                        .invalidateHttpSession(true)
                )
                .csrf(AbstractHttpConfigurer::disable) // 6. CSRF 비활성화
                .build();
    }
    // 7. 인증 관리자 관련 설정
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, BCryptPasswordEncoder bCryptPasswordEncoder, UserDetailsService userDetailsService) throws Exception {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
	// 8. 사용자 정보 서비스를 설정
        authProvider.setPasswordEncoder(bCryptPasswordEncoder);
        return new ProviderManager(authProvider);
    }
	// 9. 패스워드 인코더로 사용할 빈 등록
    @Bean 
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

```

#### 번호에 대한 설명
- 1번 : 스프링 시큐리티의 모든 기능을 사용하지 않게 하는 코드이다. 해당 코드를 작성한 이유는 인증과 인가 서비스를 모든 곳에 적용하지 않게 하기 위해서다. 일반적으로 static resources에 설정한다.
- 2번 : 특정 HTTP 요청에 대해 웹 기반 보안을 구성하는 코드이다. 이 메서드에서 인증/인가 및 로그인, 로그아웃 관련 설정을 할 수 있다.
- 3번 : 특정 경로에 대한 액세스를 설정하는 코드이다. 
	- `requestMatchers()` : 특정 요청과 일치하는 url에 대한 액세스를 설정한다.
	- `permitAll()` : 누구나 접근이 가능하게 설정한다. 즉, "/login", "/signup", "/user" 로 요청이 오면 인증/인가 없이도 접근이 가능하다.
	- `anyRequest()` : 위에서 설정한 url이외의 요청에 대해 설정한다.
	- `authenticated()` : 별도의 인가는 필요하지 않지만 인증이 성공된 상태여야 접근이 가능하다.
- 4번 : 폼 기반 로그인 설정을 진행한다.
	- `loginPage()` : 로그인 페이지 경로를 설정한다.
 	- `defaultSucessUrl()` : 로그인이 완료되었을 때 이동할 경로를 설정한다.
- 5번 : 로그아웃 설정을 진행한다.
	- `logoutSuccessUrl()` : 로그아웃이 완료되었을 때 이동할 경로를 설정한다.
	- `invalidateHttpSession()` : 로그아웃 이후에 세션을 전체 삭제할지 여부를 설정한다.
- 6번 : CSRF 설정을 비활성화한다. CSRF 공격을 방지하기 위해서는 활성화를 해야한다.
- 7번 : 인증 관리자 관련 설정을 진행한다. 사용자 정보를 가져올 서비시를 재정의하거나, 인증 방법, 예를 들어 JDBC 기반 인 등을 설정한다.
- 8번 : 사용자 서비스를 설정한다.
	- `userDetailsService()` : 사용자 정보를 가져올 서비스를 설정한다. 이때 설정하는 서비스 클래스는 UserDetailsService를 상속받은 클래스여야 한다.
	- `passwrodEncoder()` : 비밀번호를 암호화하기 위한 인코더를 설정한다.
- 9번 : 패스워드 인코더를 빈으로 등록한다.

### 6. 회원 가입 구현하기
#### 6-1. 서비스 메서드 코드 작성하기

```java
package me.shinsunyoung.springbootdeveloper.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddUserRequest {
    private String email;
    private String password;
}
```
- 사용자 정보를 담고 있는 객체를 작성한다.
```java
package me.shinsunyoung.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.springbootdeveloper.domain.User;
import me.shinsunyoung.springbootdeveloper.dto.AddUserRequest;
import me.shinsunyoung.springbootdeveloper.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public Long save(AddUserRequest dto){
        return userRepository.save(User.builder()
                .email(dto.getEmail())
                // 패스워드 암호화
                .password(bCryptPasswordEncoder.encode(dto.getPassword()))
                .build()).getId();
    }
}
```
- AddUserRequest 객체를 인수로 받는 회원 정보 추가 메서드를 작성한다. UserService를 만든다.
#### 6-2 컨트롤러 코드 작성하기
```java
package me.shinsunyoung.springbootdeveloper.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.springbootdeveloper.dto.AddUserRequest;
import me.shinsunyoung.springbootdeveloper.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@RequiredArgsConstructor
@Controller
public class UserApiController {
    private final UserService userService;

    @PostMapping("/user")
    public String signup(AddUserRequest request) {
        userService.save(request); // 회원 가입 메서드 호출
        return "redirect:/login"; // 회원 가입이 완료된 이후에 로그인 페이지로 이동한다.
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login";
    }
}

```
- 회원 가입 폼에서 회원 가입요청을 받으면 서비스 메서드를 사용하여 사용자를 저장한뒤 로그인 페이지로 redirect하는 메서드를 작성한다.

## 실제 구현한 코드의 실행 결과🔫

`http://localhost:8080/articles`로 접근하게 되면 articles는 인증된 사용자만 들어갈 수 있는 페이지이므로 로그인 페이지로 redirect 된다.
![](https://velog.velcdn.com/images/kkong_do/post/701c57f9-e0dc-46fb-adde-7f2f23711ea0/image.png)

회원가입 버튼을 누르게 되면 회원 가입 페이지로 이동하고 회원 가입 페이지는 `permitAll()` 메서드이기 때문에 별도 인증 없이 접근이 가능하다.
![](https://velog.velcdn.com/images/kkong_do/post/84ceab47-14a6-42a9-9da2-294174605228/image.png)

비밀번호가 암호화되어 저장되어있는 것을 확인할 수 있다..
![](https://velog.velcdn.com/images/kkong_do/post/a6e04813-79bc-476c-843a-22149280e0cb/image.png)

회원가입을 진행하게 되면 로그인 페이지로 이동하게 되고 가입한 이메일과 비밀번호를 입력하여 로그인을 진행한다.
![](https://velog.velcdn.com/images/kkong_do/post/a8acae50-cc02-458e-ac6c-4a6e9a6baf3b/image.png)

정상적으로 로그인이 성공하면 글 목록 페이지로 이동하게 된다.
![](https://velog.velcdn.com/images/kkong_do/post/201768c4-2843-4f5a-bc1d-dada88c3bd48/image.png)

로그아웃 버튼을 누르게 디면 인증 정보가 없으므로 다시 로그인 페이지로 이동한다.

***
## 그럼 어디에 Spring Security가 사용이 될까?🧐
>
전체 플랫폼의 로그인/로그아웃과 특정 페이지에 대한 접근을 특정 사용자에게만 해줄 때 유용하게 사용될 것 같다.

***

## 정리
>
**스프링 시큐리티**는 스프링 기반의 애플리케이션 보안(인증, 인가, 권한)을 담당하는 스프링 하위 프레임워크이며 스프링 시큐리티는 필터 기반으로 동작한다. 
각 필터에서는 인증과 인가와 관련된 작업을 처리하고 기본적으로 세션과 쿠기 방식으로 인증을 처리한다. 

- 스프링 시큐리티에서는 사용자의 인증과인가 정보를 `UserDetails` 객체에 담는다. 이 클래스를 상속받은 뒤 메서드를 오버라이드해 사용하면 된다.
- 스프링 시큐리티에서 사용자의 정보를 가져오는 데 사용하는 `UserDetailService`를 사용한다. 이 클래스를 상속받은 뒤 `loadUserByUsename()`을 오버라이드하면 스프링 시큐리티에서 사용자의 정보를 가져올 때 오버라이드된 메서드를 사용한다.

***

## 참고 문헌 및 교재📒
http://atin.tistory.com/590?pidx=0
https://taetoungs-branch.tistory.com/204?pidx=1
스프링 부트 3 백엔드 개발자 되기: 자바 편 - 신선영 -
