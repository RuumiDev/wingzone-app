import React, { useState } from 'react';
import { authService } from '../services/auth';
import { Container, Row, Col, Card, CardBody, Form, FormGroup, Label, Input, Button, Alert } from 'reactstrap';
import s from './LoginPage.module.scss';

interface LoginPageProps {
  onLogin: () => void;
}

const LoginPage: React.FC<LoginPageProps> = ({ onLogin }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await authService.login(email, password);
      onLogin();
    } catch (err) {
      setError('Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={s.root}>
      <Container>
        <Row className="justify-content-center align-items-center min-vh-100">
          <Col md={6} lg={5} xl={4}>
            <div className="text-center mb-4">
              <img src="/wingzone-logo.png" alt="WingZone" style={{ height: '80px' }} />
            </div>
            
            <Card className="shadow-lg">
              <CardBody className="p-4">
                <h2 className="text-center mb-4">WingZone Admin</h2>
                <p className="text-center text-muted mb-4">Sign in to access the admin panel</p>

                {error && (
                  <Alert color="danger" className="mb-3">
                    <i className="bi bi-exclamation-triangle me-2"></i>
                    {error}
                  </Alert>
                )}

                <Form onSubmit={handleSubmit}>
                  <FormGroup>
                    <Label for="email">
                      <i className="bi bi-envelope me-2"></i>
                      Email Address
                    </Label>
                    <Input
                      type="email"
                      id="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="Enter your email"
                      required
                    />
                  </FormGroup>

                  <FormGroup>
                    <Label for="password">
                      <i className="bi bi-lock me-2"></i>
                      Password
                    </Label>
                    <Input
                      type="password"
                      id="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Enter your password"
                      required
                    />
                  </FormGroup>

                  <Button
                    type="submit"
                    color="primary"
                    size="lg"
                    block
                    disabled={loading}
                    className="mt-3"
                  >
                    {loading ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2"></span>
                        Signing in...
                      </>
                    ) : (
                      <>
                        <i className="bi bi-box-arrow-in-right me-2"></i>
                        Sign In
                      </>
                    )}
                  </Button>
                </Form>
              </CardBody>
            </Card>

            <p className="text-center text-muted mt-3 small">
              © 2025 WingZone. All rights reserved.
            </p>
          </Col>
        </Row>
      </Container>
    </div>
  );
};

export default LoginPage;
