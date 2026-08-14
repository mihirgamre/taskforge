import { Component, type ErrorInfo, type ReactNode } from 'react';

type Props = {
  children: ReactNode;
};

type State = {
  hasError: boolean;
};

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Application error boundary caught an error.', { error, info });
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="mx-auto max-w-3xl p-8">
          <h1 className="text-2xl font-semibold">TaskForge could not load this view.</h1>
          <p className="mt-3 text-slate-700">Refresh the page. If the problem continues, use the request details from backend logs.</p>
        </main>
      );
    }

    return this.props.children;
  }
}

